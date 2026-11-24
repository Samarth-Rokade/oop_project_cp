package dao;

import config.DatabaseConfig;
import exception.DataAccessException;
import model.ListingStatus;
import model.Order;
import model.OrderItem;
import model.OrderStatus;
import model.PaymentMethod;
import model.PaymentStatus;
import model.Product;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for orders, order items and payments.
 *
 * Methods that take a {@link Connection} are steps of a larger
 * transaction: the caller (OrderService) owns the connection and decides
 * when to COMMIT or ROLLBACK. They never commit on their own.
 */
public class OrderDAO {

    /** A product row locked for the current transaction, plus whether its seller may sell. */
    public record LockedProduct(Product product, boolean sellerCanSell) {
    }

    // ------------------------------------------------------------ transaction steps

    /**
     * SELECT ... FOR UPDATE takes an exclusive row lock on the product
     * until the transaction ends, so two buyers cannot both buy the last
     * units: the second one waits and then sees the reduced stock.
     */
    public Optional<LockedProduct> lockProduct(Connection con, int productId) throws SQLException {
        String sql = "SELECT p.product_id, p.product_name, p.unit_code, p.available_quantity, p.price_per_unit, "
                   + "       p.listing_status, p.farmer_id, f.farm_name, "
                   + "       (f.verification_status = 'VERIFIED' AND u.account_status = 'ACTIVE') AS seller_ok "
                   + "FROM product p "
                   + "JOIN farmer_profile f ON f.farmer_id = p.farmer_id "
                   + "JOIN users u ON u.user_id = p.farmer_id "
                   + "WHERE p.product_id = ? "
                   + "FOR UPDATE OF p";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Product p = new Product();
                p.setProductId(rs.getInt("product_id"));
                p.setProductName(rs.getString("product_name"));
                p.setUnitCode(rs.getString("unit_code"));
                p.setAvailableQuantity(rs.getBigDecimal("available_quantity"));
                p.setPricePerUnit(rs.getBigDecimal("price_per_unit"));
                p.setListingStatus(ListingStatus.valueOf(rs.getString("listing_status")));
                p.setFarmerId(rs.getInt("farmer_id"));
                p.setFarmName(rs.getString("farm_name"));
                return Optional.of(new LockedProduct(p, rs.getBoolean("seller_ok")));
            }
        }
    }

    public int insertOrder(Connection con, int buyerId, int addressId, BigDecimal total) throws SQLException {
        String sql = "INSERT INTO orders (buyer_id, delivery_address_id, order_status, total_amount) "
                   + "VALUES (?, ?, 'PLACED', ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, buyerId);
            ps.setInt(2, addressId);
            ps.setBigDecimal(3, total);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public void insertItem(Connection con, int orderId, int productId, BigDecimal quantity, BigDecimal unitPrice)
            throws SQLException {
        String sql = "INSERT INTO order_item (order_id, product_id, quantity, unit_price_at_order) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, productId);
            ps.setBigDecimal(3, quantity);
            ps.setBigDecimal(4, unitPrice);
            ps.executeUpdate();
        }
    }

    /**
     * Reduces stock; an ACTIVE product that reaches zero becomes OUT_OF_STOCK.
     * listing_status is assigned first because MySQL evaluates SET
     * assignments left to right.
     */
    public void reduceStock(Connection con, int productId, BigDecimal quantity) throws SQLException {
        String sql = "UPDATE product SET "
                   + "  listing_status = CASE WHEN available_quantity - ? <= 0 AND listing_status = 'ACTIVE' "
                   + "                        THEN 'OUT_OF_STOCK' ELSE listing_status END, "
                   + "  available_quantity = available_quantity - ? "
                   + "WHERE product_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, quantity);
            ps.setBigDecimal(2, quantity);
            ps.setInt(3, productId);
            ps.executeUpdate();
        }
    }

    /** Gives stock back (cancel/reject); OUT_OF_STOCK becomes ACTIVE again. */
    public void restoreStock(Connection con, int productId, BigDecimal quantity) throws SQLException {
        String sql = "UPDATE product SET "
                   + "  listing_status = CASE WHEN listing_status = 'OUT_OF_STOCK' THEN 'ACTIVE' ELSE listing_status END, "
                   + "  available_quantity = available_quantity + ? "
                   + "WHERE product_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, quantity);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    public void insertPayment(Connection con, int orderId, PaymentMethod method, BigDecimal amount,
                              PaymentStatus status, String reference) throws SQLException {
        String sql = "INSERT INTO payment (order_id, payment_method, amount, payment_status, payment_date, "
                   + "transaction_reference) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, method.name());
            ps.setBigDecimal(3, amount);
            ps.setString(4, status.name());
            if (status == PaymentStatus.PAID) {
                ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            } else {
                ps.setNull(5, Types.TIMESTAMP);
            }
            if (reference == null) {
                ps.setNull(6, Types.VARCHAR);
            } else {
                ps.setString(6, reference);
            }
            ps.executeUpdate();
        }
    }

    /** Locks the order row and returns its owner and status. */
    public Optional<Order> lockOrder(Connection con, int orderId) throws SQLException {
        String sql = "SELECT order_id, buyer_id, order_status FROM orders WHERE order_id = ? FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Order o = new Order();
                o.setOrderId(rs.getInt("order_id"));
                o.setBuyerId(rs.getInt("buyer_id"));
                o.setStatus(OrderStatus.valueOf(rs.getString("order_status")));
                return Optional.of(o);
            }
        }
    }

    public void updateStatus(Connection con, int orderId, OrderStatus status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE orders SET order_status = ? WHERE order_id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    /** Moves this order's payments from one status to another (e.g. PAID -> REFUNDED). */
    public void updatePaymentStatus(Connection con, int orderId, PaymentStatus from, PaymentStatus to)
            throws SQLException {
        String sql = "UPDATE payment SET payment_status = ? WHERE order_id = ? AND payment_status = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, to.name());
            ps.setInt(2, orderId);
            ps.setString(3, from.name());
            ps.executeUpdate();
        }
    }

    public List<OrderItem> findItems(Connection con, int orderId) throws SQLException {
        String sql = "SELECT oi.order_item_id, oi.order_id, oi.product_id, p.product_name, p.unit_code, "
                   + "       f.farm_name, oi.quantity, oi.unit_price_at_order, oi.line_total "
                   + "FROM order_item oi "
                   + "JOIN product p ON p.product_id = oi.product_id "
                   + "JOIN farmer_profile f ON f.farmer_id = p.farmer_id "
                   + "WHERE oi.order_id = ? ORDER BY oi.order_item_id";
        List<OrderItem> items = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem i = new OrderItem();
                    i.setOrderItemId(rs.getInt("order_item_id"));
                    i.setOrderId(rs.getInt("order_id"));
                    i.setProductId(rs.getInt("product_id"));
                    i.setProductName(rs.getString("product_name"));
                    i.setUnitCode(rs.getString("unit_code"));
                    i.setFarmName(rs.getString("farm_name"));
                    i.setQuantity(rs.getBigDecimal("quantity"));
                    i.setUnitPriceAtOrder(rs.getBigDecimal("unit_price_at_order"));
                    i.setLineTotal(rs.getBigDecimal("line_total"));
                    items.add(i);
                }
            }
        }
        return items;
    }

    // ------------------------------------------------------------ simple reads

    public List<OrderItem> findItems(int orderId) {
        try (Connection con = DatabaseConfig.getConnection()) {
            return findItems(con, orderId);
        } catch (SQLException e) {
            throw new DataAccessException("Could not load items of order #" + orderId, e);
        }
    }

    /** A buyer's orders, newest first, with farm, payment and delivery summary. */
    public List<Order> findByBuyer(int buyerId) {
        String sql = "SELECT o.order_id, o.buyer_id, o.order_date, o.order_status, o.total_amount, "
                   + "       o.delivery_address_id, "
                   + "       CONCAT(a.address_line, ', ', a.city, ' - ', a.postal_code) AS deliver_to, "
                   + "       (SELECT COUNT(*) FROM order_item oi WHERE oi.order_id = o.order_id) AS item_count, "
                   + "       (SELECT GROUP_CONCAT(DISTINCT f.farm_name ORDER BY f.farm_name SEPARATOR ', ') "
                   + "          FROM order_item oi "
                   + "          JOIN product p ON p.product_id = oi.product_id "
                   + "          JOIN farmer_profile f ON f.farmer_id = p.farmer_id "
                   + "         WHERE oi.order_id = o.order_id) AS farm_names, "
                   + "       pm.payment_method, pm.payment_status, d.delivery_status "
                   + "FROM orders o "
                   + "JOIN address a ON a.address_id = o.delivery_address_id "
                   + "LEFT JOIN payment pm ON pm.payment_id = "
                   + "     (SELECT MAX(p2.payment_id) FROM payment p2 WHERE p2.order_id = o.order_id) "
                   + "LEFT JOIN delivery d ON d.order_id = o.order_id "
                   + "WHERE o.buyer_id = ? "
                   + "ORDER BY o.order_date DESC, o.order_id DESC";
        List<Order> orders = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order o = new Order();
                    o.setOrderId(rs.getInt("order_id"));
                    o.setBuyerId(rs.getInt("buyer_id"));
                    o.setOrderDate(rs.getTimestamp("order_date").toLocalDateTime());
                    o.setStatus(OrderStatus.valueOf(rs.getString("order_status")));
                    o.setTotalAmount(rs.getBigDecimal("total_amount"));
                    o.setDeliveryAddressId(rs.getInt("delivery_address_id"));
                    o.setDeliverTo(rs.getString("deliver_to"));
                    o.setItemCount(rs.getInt("item_count"));
                    o.setFarmNames(rs.getString("farm_names"));
                    String method = rs.getString("payment_method");
                    o.setPaymentMethod(method == null ? null : PaymentMethod.valueOf(method));
                    String payStatus = rs.getString("payment_status");
                    o.setPaymentStatus(payStatus == null ? null : PaymentStatus.valueOf(payStatus));
                    o.setDeliveryStatus(rs.getString("delivery_status"));
                    orders.add(o);
                }
            }
            return orders;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load orders", e);
        }
    }
}
