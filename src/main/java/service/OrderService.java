package service;

import config.DatabaseConfig;
import dao.AddressDAO;
import dao.AuditDAO;
import dao.OrderDAO;
import exception.DataAccessException;
import exception.InsufficientStockException;
import exception.InvalidOrderStatusException;
import exception.ValidationException;
import interfaces.OrderOperations;
import model.Buyer;
import model.Cart;
import model.CartItem;
import model.Farmer;
import model.ListingStatus;
import model.Order;
import model.OrderItem;
import model.OrderStatus;
import model.PaymentMethod;
import model.PaymentStatus;
import model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Order placement and cancellation.
 *
 * Both are database transactions (spec 14.2): either every step succeeds
 * and is COMMITted, or any failure ROLLs BACK everything, so stock and
 * orders can never get out of step.
 */
public class OrderService implements OrderOperations {

    private final OrderDAO orderDAO;
    private final AddressDAO addressDAO;
    private final AuditDAO auditDAO;

    public OrderService() {
        this(new OrderDAO(), new AddressDAO(), new AuditDAO());
    }

    public OrderService(OrderDAO orderDAO, AddressDAO addressDAO, AuditDAO auditDAO) {
        this.orderDAO = orderDAO;
        this.addressDAO = addressDAO;
        this.auditDAO = auditDAO;
    }

    /**
     * Transaction steps:
     * <ol>
     *   <li>Lock every product row (SELECT ... FOR UPDATE), in product-id order
     *       so two transactions always lock in the same order (avoids deadlock).</li>
     *   <li>Re-check each product: still on sale, seller verified, price unchanged,
     *       enough stock.</li>
     *   <li>For each farm: insert the order with a total calculated from database
     *       prices (never from the client), insert its items, reduce stock,
     *       record the (simulated) payment.</li>
     *   <li>COMMIT - or ROLLBACK if anything above failed.</li>
     * </ol>
     */
    @Override
    public List<Integer> placeOrder(Buyer buyer, Cart cart, int addressId, PaymentMethod method)
            throws ValidationException {
        if (cart == null || cart.isEmpty()) {
            throw new ValidationException("Your cart is empty.");
        }
        if (method == null) {
            throw new ValidationException("Choose a payment method.");
        }
        boolean ownsAddress = addressDAO.findByUser(buyer.getUserId()).stream()
                .anyMatch(a -> a.getAddressId() == addressId);
        if (!ownsAddress) {
            throw new ValidationException("Choose one of your delivery addresses.");
        }

        List<CartItem> items = cart.getItems().stream()
                .sorted(Comparator.comparingInt(i -> i.getProduct().getProductId()))
                .collect(Collectors.toList());

        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);                                   // START TRANSACTION
            try {
                // Steps 1 + 2: lock and re-check
                Map<Integer, Product> current = new LinkedHashMap<>();
                for (CartItem item : items) {
                    Product inCart = item.getProduct();
                    OrderDAO.LockedProduct locked = orderDAO.lockProduct(con, inCart.getProductId())
                            .orElseThrow(() -> new ValidationException(
                                    inCart.getProductName() + " is no longer available."));
                    checkStillOrderable(locked, item);
                    current.put(inCart.getProductId(), locked.product());
                }

                // Step 3: one order per farm
                Map<Integer, List<CartItem>> byFarmer = items.stream()
                        .collect(Collectors.groupingBy(
                                i -> current.get(i.getProduct().getProductId()).getFarmerId(),
                                LinkedHashMap::new, Collectors.toList()));

                List<Integer> orderIds = new ArrayList<>();
                for (List<CartItem> farmItems : byFarmer.values()) {
                    BigDecimal total = farmItems.stream()
                            .map(i -> lineTotal(i.getQuantity(), current.get(i.getProduct().getProductId())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    int orderId = orderDAO.insertOrder(con, buyer.getUserId(), addressId, total);
                    for (CartItem i : farmItems) {
                        Product p = current.get(i.getProduct().getProductId());
                        orderDAO.insertItem(con, orderId, p.getProductId(), i.getQuantity(), p.getPricePerUnit());
                        orderDAO.reduceStock(con, p.getProductId(), i.getQuantity());
                    }
                    PaymentStatus payStatus = method.isPaidOnline() ? PaymentStatus.PAID : PaymentStatus.PENDING;
                    String reference = method.isPaidOnline() ? String.format("SIM-%s-%06d", method.name(), orderId) : null;
                    orderDAO.insertPayment(con, orderId, method, total, payStatus, reference);
                    orderIds.add(orderId);
                }

                con.commit();                                           // COMMIT
                return orderIds;
            } catch (ValidationException | SQLException | RuntimeException e) {
                con.rollback();                                         // ROLLBACK
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not place the order", e);
        }
    }

    /**
     * Cancels an order if its status allows it, restores stock and
     * refunds/cancels the payment - all in one transaction.
     */
    @Override
    public void cancelOrder(Buyer buyer, int orderId) throws ValidationException {
        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                Order order = orderDAO.lockOrder(con, orderId)
                        .filter(o -> o.getBuyerId() == buyer.getUserId())
                        .orElseThrow(() -> new ValidationException("Order not found."));
                if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
                    throw new InvalidOrderStatusException(orderId, order.getStatus(), OrderStatus.CANCELLED);
                }

                releaseOrder(con, orderId);
                orderDAO.updateStatus(con, orderId, OrderStatus.CANCELLED);

                con.commit();
            } catch (ValidationException | SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not cancel order #" + orderId, e);
        }
    }

    @Override
    public List<Order> getOrdersOf(Buyer buyer) {
        return orderDAO.findByBuyer(buyer.getUserId());
    }

    @Override
    public List<OrderItem> getItems(Buyer buyer, Order order) throws ValidationException {
        if (order.getBuyerId() != buyer.getUserId()) {
            throw new ValidationException("Order not found.");
        }
        return orderDAO.findItems(order.getOrderId());
    }

    // ------------------------------------------------------------------ farmer side

    @Override
    public List<Order> getOrdersOf(Farmer farmer) {
        return orderDAO.findForFarmer(farmer.getUserId());
    }

    @Override
    public List<OrderItem> getItems(Farmer farmer, Order order) throws ValidationException {
        try (Connection con = DatabaseConfig.getConnection()) {
            if (!orderDAO.findFarmerIds(con, order.getOrderId()).contains(farmer.getUserId())) {
                throw new ValidationException("Order not found.");
            }
            return orderDAO.findItems(con, order.getOrderId());
        } catch (SQLException e) {
            throw new DataAccessException("Could not load items of order #" + order.getOrderId(), e);
        }
    }

    @Override
    public int countNewOrders(Farmer farmer) {
        return orderDAO.countAwaitingFarmer(farmer.getUserId());
    }

    /** PLACED -> CONFIRMED. A delivery record is opened. */
    @Override
    public void acceptOrder(Farmer farmer, int orderId) throws ValidationException {
        transition(farmer, orderId, OrderStatus.CONFIRMED,
                (con, order) -> orderDAO.saveDelivery(con, orderId, "PENDING", null, null, null, null));
    }

    /** PLACED -> REJECTED. Stock goes back, payment is refunded/cancelled, reason is logged. */
    @Override
    public void rejectOrder(Farmer farmer, int orderId, String reason) throws ValidationException {
        String why = requireReason(reason);
        transition(farmer, orderId, OrderStatus.REJECTED, (con, order) -> {
            releaseOrder(con, orderId);
            auditDAO.log(con, farmer.getUserId(), "REJECT_ORDER", "ORDER", orderId, "Reason: " + why);
        });
    }

    /** CONFIRMED -> PACKED. */
    @Override
    public void markPacked(Farmer farmer, int orderId) throws ValidationException {
        transition(farmer, orderId, OrderStatus.PACKED,
                (con, order) -> orderDAO.saveDelivery(con, orderId, "PACKED", null, null, null, null));
    }

    /** PACKED -> DISPATCHED, with delivery partner and optional tracking number. */
    @Override
    public void dispatchOrder(Farmer farmer, int orderId, String deliveryPartner, String trackingReference)
            throws ValidationException {
        String partner = deliveryPartner == null ? "" : deliveryPartner.trim();
        String tracking = trackingReference == null || trackingReference.isBlank() ? null : trackingReference.trim();
        if (partner.isEmpty()) {
            throw new ValidationException("Enter the delivery partner (e.g. your own vehicle or a courier name).");
        }
        if (partner.length() > 80) {
            throw new ValidationException("Delivery partner must be at most 80 characters.");
        }
        if (tracking != null && tracking.length() > 40) {
            throw new ValidationException("Tracking reference must be at most 40 characters.");
        }
        transition(farmer, orderId, OrderStatus.DISPATCHED, (con, order) ->
                orderDAO.saveDelivery(con, orderId, "DISPATCHED", LocalDate.now(), null, partner, tracking));
    }

    /** DISPATCHED -> DELIVERED. Cash on delivery is now collected, so a pending payment becomes PAID. */
    @Override
    public void markDelivered(Farmer farmer, int orderId) throws ValidationException {
        transition(farmer, orderId, OrderStatus.DELIVERED, (con, order) -> {
            orderDAO.saveDelivery(con, orderId, "DELIVERED", null, LocalDate.now(), null, null);
            orderDAO.updatePaymentStatus(con, orderId, PaymentStatus.PENDING, PaymentStatus.PAID);
        });
    }

    /**
     * DISPATCHED -> DELIVERY_FAILED. Payment is refunded/cancelled.
     * Stock is NOT restored automatically: the goods left the farm, so the
     * farmer updates stock by hand if they come back in sellable condition.
     */
    @Override
    public void markDeliveryFailed(Farmer farmer, int orderId, String reason) throws ValidationException {
        String why = requireReason(reason);
        transition(farmer, orderId, OrderStatus.DELIVERY_FAILED, (con, order) -> {
            orderDAO.saveDelivery(con, orderId, "FAILED", null, null, null, null);
            orderDAO.updatePaymentStatus(con, orderId, PaymentStatus.PAID, PaymentStatus.REFUNDED);
            orderDAO.updatePaymentStatus(con, orderId, PaymentStatus.PENDING, PaymentStatus.CANCELLED);
            auditDAO.log(con, farmer.getUserId(), "DELIVERY_FAILED", "ORDER", orderId, "Reason: " + why);
        });
    }

    /** Extra work done inside a status-change transaction. */
    @FunctionalInterface
    private interface TransactionStep {
        void run(Connection con, Order lockedOrder) throws SQLException, ValidationException;
    }

    /**
     * One status change as a transaction:
     * lock the order row -> check the farmer owns it -> check the
     * transition is allowed -> run the extra step -> update status -> COMMIT.
     * Locking the order first means a buyer cancelling and a farmer
     * accepting at the same moment are handled one after the other.
     */
    private void transition(Farmer farmer, int orderId, OrderStatus next, TransactionStep step)
            throws ValidationException {
        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                Order order = orderDAO.lockOrder(con, orderId)
                        .orElseThrow(() -> new ValidationException("Order not found."));
                List<Integer> farmers = orderDAO.findFarmerIds(con, orderId);
                if (!farmers.equals(List.of(farmer.getUserId()))) {
                    throw new ValidationException("Order not found in your orders.");
                }
                if (!order.getStatus().canTransitionTo(next)) {
                    throw new InvalidOrderStatusException(orderId, order.getStatus(), next);
                }
                step.run(con, order);
                orderDAO.updateStatus(con, orderId, next);
                con.commit();
            } catch (ValidationException | SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not update order #" + orderId, e);
        }
    }

    /** Shared by cancel and reject: give stock back, settle the payment, drop the delivery. */
    private void releaseOrder(Connection con, int orderId) throws SQLException {
        List<OrderItem> items = orderDAO.findItems(con, orderId);
        items.sort(Comparator.comparingInt(OrderItem::getProductId));    // same lock order as placeOrder
        for (OrderItem item : items) {
            orderDAO.restoreStock(con, item.getProductId(), item.getQuantity());
        }
        orderDAO.updatePaymentStatus(con, orderId, PaymentStatus.PAID, PaymentStatus.REFUNDED);
        orderDAO.updatePaymentStatus(con, orderId, PaymentStatus.PENDING, PaymentStatus.CANCELLED);
        orderDAO.deleteDelivery(con, orderId);
    }

    private static String requireReason(String reason) throws ValidationException {
        String r = reason == null ? "" : reason.trim();
        if (r.isEmpty()) {
            throw new ValidationException("Please give a short reason.");
        }
        if (r.length() > 200) {
            throw new ValidationException("Reason must be at most 200 characters.");
        }
        return r;
    }

    // ------------------------------------------------------------------

    private static void checkStillOrderable(OrderDAO.LockedProduct locked, CartItem item) throws ValidationException {
        Product p = locked.product();
        Product inCart = item.getProduct();
        if (p.getListingStatus() != ListingStatus.ACTIVE || !locked.sellerCanSell()) {
            throw new ValidationException(p.getProductName() + " is no longer available. Please remove it from your cart.");
        }
        if (p.getPricePerUnit().compareTo(inCart.getPricePerUnit()) != 0) {
            throw new ValidationException("The price of " + p.getProductName() + " changed from "
                    + inCart.getPricePerUnit().toPlainString() + " to " + p.getPricePerUnit().toPlainString()
                    + " per " + p.getUnitCode() + ". Please review your cart and try again.");
        }
        if (item.getQuantity().compareTo(p.getAvailableQuantity()) > 0) {
            throw new InsufficientStockException(p.getProductId(), p.getProductName(),
                    item.getQuantity(), p.getAvailableQuantity(), p.getUnitCode());
        }
    }

    /** Same rounding as the order_item.line_total generated column: ROUND(q * p, 2). */
    static BigDecimal lineTotal(BigDecimal quantity, Product product) {
        return quantity.multiply(product.getPricePerUnit()).setScale(2, RoundingMode.HALF_UP);
    }
}
