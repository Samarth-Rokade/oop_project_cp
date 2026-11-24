package dao;

import config.DatabaseConfig;
import exception.DataAccessException;
import model.AccountStatus;
import model.AuditEntry;
import model.DashboardStats;
import model.Farmer;
import model.ListingStatus;
import model.OrderStatus;
import model.Payment;
import model.PaymentMethod;
import model.PaymentStatus;
import model.Product;
import model.Role;
import model.UserSummary;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Queries and updates used only by the administrator screens.
 * Methods that take a Connection run inside the caller's transaction.
 */
public class AdminDAO {

    // ------------------------------------------------------------------ overview

    public DashboardStats loadStats() {
        String sql = "SELECT "
                   + " (SELECT COUNT(*) FROM users WHERE role = 'FARMER') AS farmers, "
                   + " (SELECT COUNT(*) FROM users WHERE role = 'BUYER') AS buyers, "
                   + " (SELECT COUNT(*) FROM farmer_profile WHERE verification_status = 'PENDING') AS pending, "
                   + " (SELECT COUNT(*) FROM active_product_view) AS live, "
                   + " (SELECT COUNT(*) FROM product WHERE available_quantity > 0 AND available_quantity < 10 "
                   + "     AND listing_status <> 'REMOVED') AS low_stock, "
                   + " (SELECT COUNT(*) FROM orders WHERE order_status IN ('PLACED','CONFIRMED','PACKED','DISPATCHED')) AS open_orders, "
                   + " (SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE order_status = 'DELIVERED') AS delivered_value, "
                   + " (SELECT COALESCE(SUM(amount), 0) FROM payment WHERE payment_status = 'PAID') AS paid_value";
        String byStatus = "SELECT order_status, COUNT(*) FROM orders GROUP BY order_status";
        try (Connection con = DatabaseConfig.getConnection()) {
            Map<OrderStatus, Integer> counts = new EnumMap<>(OrderStatus.class);
            for (OrderStatus s : OrderStatus.values()) {
                counts.put(s, 0);
            }
            try (PreparedStatement ps = con.prepareStatement(byStatus);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(OrderStatus.valueOf(rs.getString(1)), rs.getInt(2));
                }
            }
            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new DashboardStats(rs.getInt("farmers"), rs.getInt("buyers"), rs.getInt("pending"),
                        rs.getInt("live"), rs.getInt("low_stock"), rs.getInt("open_orders"),
                        rs.getBigDecimal("delivered_value"), rs.getBigDecimal("paid_value"), counts);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load dashboard figures", e);
        }
    }

    // ------------------------------------------------------------------ users

    public List<UserSummary> findAllUsers() {
        String sql = "SELECT u.user_id, u.full_name, u.email, u.phone, u.role, u.account_status, u.created_at, "
                   + "       f.verification_status, f.farm_name, b.buyer_type "
                   + "FROM users u "
                   + "LEFT JOIN farmer_profile f ON f.farmer_id = u.user_id "
                   + "LEFT JOIN buyer_profile b ON b.buyer_id = u.user_id "
                   + "ORDER BY (f.verification_status = 'PENDING') DESC, u.created_at DESC";
        List<UserSummary> users = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load users", e);
        }
    }

    /** Locks the user row for a status change and returns its current state. */
    public UserSummary lockUser(Connection con, int userId) throws SQLException {
        String sql = "SELECT u.user_id, u.full_name, u.email, u.phone, u.role, u.account_status, u.created_at, "
                   + "       f.verification_status, f.farm_name, b.buyer_type "
                   + "FROM users u "
                   + "LEFT JOIN farmer_profile f ON f.farmer_id = u.user_id "
                   + "LEFT JOIN buyer_profile b ON b.buyer_id = u.user_id "
                   + "WHERE u.user_id = ? FOR UPDATE OF u";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        }
    }

    public void setAccountStatus(Connection con, int userId, AccountStatus status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE users SET account_status = ? WHERE user_id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void setVerification(Connection con, int farmerId, Farmer.VerificationStatus status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE farmer_profile SET verification_status = ? WHERE farmer_id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, farmerId);
            ps.executeUpdate();
        }
    }

    private static UserSummary mapUser(ResultSet rs) throws SQLException {
        String verification = rs.getString("verification_status");
        return new UserSummary(
                rs.getInt("user_id"), rs.getString("full_name"), rs.getString("email"), rs.getString("phone"),
                Role.valueOf(rs.getString("role")), AccountStatus.valueOf(rs.getString("account_status")),
                verification == null ? null : Farmer.VerificationStatus.valueOf(verification),
                rs.getString("farm_name"), rs.getString("buyer_type"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    // ------------------------------------------------------------------ products

    public List<Product> findAllProducts() {
        String sql = "SELECT p.product_id, p.farmer_id, p.category_id, c.category_name, p.product_name, "
                   + "       p.description, p.unit_code, p.available_quantity, p.price_per_unit, p.harvest_date, "
                   + "       p.listing_status, p.updated_at, f.farm_name, u.full_name "
                   + "FROM product p "
                   + "JOIN category c ON c.category_id = p.category_id "
                   + "JOIN farmer_profile f ON f.farmer_id = p.farmer_id "
                   + "JOIN users u ON u.user_id = p.farmer_id "
                   + "ORDER BY f.farm_name, p.product_name";
        List<Product> products = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Product p = new Product();
                p.setProductId(rs.getInt("product_id"));
                p.setFarmerId(rs.getInt("farmer_id"));
                p.setCategoryId(rs.getInt("category_id"));
                p.setCategoryName(rs.getString("category_name"));
                p.setProductName(rs.getString("product_name"));
                p.setDescription(rs.getString("description"));
                p.setUnitCode(rs.getString("unit_code"));
                p.setAvailableQuantity(rs.getBigDecimal("available_quantity"));
                p.setPricePerUnit(rs.getBigDecimal("price_per_unit"));
                Date harvest = rs.getDate("harvest_date");
                p.setHarvestDate(harvest == null ? null : harvest.toLocalDate());
                p.setListingStatus(ListingStatus.valueOf(rs.getString("listing_status")));
                p.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                p.setFarmName(rs.getString("farm_name"));
                p.setFarmerName(rs.getString("full_name"));
                products.add(p);
            }
            return products;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load products", e);
        }
    }

    /** Returns the current status after locking the row, or null if the product does not exist. */
    public ListingStatus lockProductStatus(Connection con, int productId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT listing_status FROM product WHERE product_id = ? FOR UPDATE")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? ListingStatus.valueOf(rs.getString(1)) : null;
            }
        }
    }

    public void setListingStatus(Connection con, int productId, ListingStatus status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE product SET listing_status = ? WHERE product_id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ payments

    public List<Payment> findAllPayments() {
        String sql = "SELECT pm.payment_id, pm.order_id, u.full_name, pm.payment_method, pm.amount, "
                   + "       pm.payment_status, pm.payment_date, pm.transaction_reference "
                   + "FROM payment pm "
                   + "JOIN orders o ON o.order_id = pm.order_id "
                   + "JOIN users u ON u.user_id = o.buyer_id "
                   + "ORDER BY pm.payment_id DESC";
        List<Payment> payments = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp date = rs.getTimestamp("payment_date");
                payments.add(new Payment(rs.getInt("payment_id"), rs.getInt("order_id"), rs.getString("full_name"),
                        PaymentMethod.valueOf(rs.getString("payment_method")), rs.getBigDecimal("amount"),
                        PaymentStatus.valueOf(rs.getString("payment_status")),
                        date == null ? null : date.toLocalDateTime(), rs.getString("transaction_reference")));
            }
            return payments;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load payments", e);
        }
    }

    // ------------------------------------------------------------------ audit log

    /**
     * Latest audit entries, optionally filtered by action and a text search
     * over the user's name and the details.
     */
    public List<AuditEntry> findAudit(String action, String text, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT a.log_id, a.action_time, u.full_name, a.action, a.entity_type, a.entity_id, a.details "
              + "FROM audit_log a LEFT JOIN users u ON u.user_id = a.user_id WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (action != null) {
            sql.append(" AND a.action = ?");
            params.add(action);
        }
        if (text != null && !text.isBlank()) {
            sql.append(" AND (u.full_name LIKE ? OR a.details LIKE ? OR a.entity_type LIKE ?)");
            String like = "%" + CatalogDAO.escapeLike(text.trim()) + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY a.action_time DESC, a.log_id DESC LIMIT ?");
        params.add(limit);

        List<AuditEntry> entries = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int entity = rs.getInt("entity_id");
                    entries.add(new AuditEntry(rs.getLong("log_id"), rs.getTimestamp("action_time").toLocalDateTime(),
                            rs.getString("full_name"), rs.getString("action"), rs.getString("entity_type"),
                            rs.wasNull() ? null : entity, rs.getString("details")));
                }
            }
            return entries;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the activity log", e);
        }
    }

    public List<String> findAuditActions() {
        List<String> actions = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT DISTINCT action FROM audit_log ORDER BY action");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                actions.add(rs.getString(1));
            }
            return actions;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load activity types", e);
        }
    }
}
