package service;

import config.DatabaseConfig;
import dao.AddressDAO;
import dao.OrderDAO;
import exception.DataAccessException;
import exception.InsufficientStockException;
import exception.InvalidOrderStatusException;
import exception.ValidationException;
import interfaces.OrderOperations;
import model.Buyer;
import model.Cart;
import model.CartItem;
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

    public OrderService() {
        this(new OrderDAO(), new AddressDAO());
    }

    public OrderService(OrderDAO orderDAO, AddressDAO addressDAO) {
        this.orderDAO = orderDAO;
        this.addressDAO = addressDAO;
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

                List<OrderItem> items = orderDAO.findItems(con, orderId);
                items.sort(Comparator.comparingInt(OrderItem::getProductId));    // same lock order as placeOrder
                for (OrderItem item : items) {
                    orderDAO.restoreStock(con, item.getProductId(), item.getQuantity());
                }
                orderDAO.updateStatus(con, orderId, OrderStatus.CANCELLED);
                orderDAO.updatePaymentStatus(con, orderId, PaymentStatus.PAID, PaymentStatus.REFUNDED);
                orderDAO.updatePaymentStatus(con, orderId, PaymentStatus.PENDING, PaymentStatus.CANCELLED);

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
