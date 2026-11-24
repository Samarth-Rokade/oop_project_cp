package dao;

import config.DatabaseConfig;
import exception.DataAccessException;
import model.Farmer;
import model.ListingStatus;
import model.OrderStatus;
import model.Reports.CategorySales;
import model.Reports.FarmerSales;
import model.Reports.ProductSales;
import model.Reports.SaleLine;
import model.Reports.SalesSummary;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reporting queries (GROUP BY, outer joins, aggregates).
 *
 * The period filter is "o.order_date >= ?". For "all time" the start date
 * is 1900-01-01 so every query keeps the same shape.
 */
public class ReportDAO {

    /** Orders that count as sales. */
    private static final String COUNTED = "o.order_status NOT IN ('REJECTED', 'CANCELLED', 'DELIVERY_FAILED')";
    private static final LocalDate BEGINNING = LocalDate.of(1900, 1, 1);

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i] instanceof LocalDate d ? Date.valueOf(d) : params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the report", e);
        }
    }

    private static LocalDate from(LocalDate start) {
        return start == null ? BEGINNING : start;
    }

    // ------------------------------------------------------------------ farmer

    public SalesSummary farmerSummary(int farmerId, LocalDate start) {
        String sql = "SELECT COUNT(DISTINCT o.order_id) AS orders, "
                   + "       COALESCE(SUM(oi.line_total), 0) AS sales, "
                   + "       COALESCE(SUM(CASE WHEN o.order_status = 'DELIVERED' THEN oi.line_total END), 0) AS delivered "
                   + "FROM order_item oi "
                   + "JOIN orders o ON o.order_id = oi.order_id "
                   + "JOIN product p ON p.product_id = oi.product_id "
                   + "WHERE p.farmer_id = ? AND " + COUNTED + " AND o.order_date >= ?";
        return query(sql, rs -> new SalesSummary(rs.getInt("orders"), rs.getBigDecimal("sales"),
                rs.getBigDecimal("delivered")), farmerId, from(start)).get(0);
    }

    /** Every product of the farmer, including ones that sold nothing (LEFT JOIN). */
    public List<ProductSales> farmerProducts(int farmerId, LocalDate start) {
        return productSales("WHERE p.farmer_id = ? ", start, farmerId);
    }

    public List<SaleLine> farmerSaleLines(int farmerId, LocalDate start) {
        String sql = "SELECT o.order_id, o.order_date, u.full_name, p.product_name, oi.quantity, p.unit_code, "
                   + "       oi.unit_price_at_order, oi.line_total, o.order_status "
                   + "FROM order_item oi "
                   + "JOIN orders o ON o.order_id = oi.order_id "
                   + "JOIN product p ON p.product_id = oi.product_id "
                   + "JOIN users u ON u.user_id = o.buyer_id "
                   + "WHERE p.farmer_id = ? AND o.order_date >= ? "
                   + "ORDER BY o.order_date DESC, o.order_id DESC";
        return query(sql, rs -> new SaleLine(rs.getInt("order_id"), rs.getTimestamp("order_date").toLocalDateTime(),
                rs.getString("full_name"), rs.getString("product_name"), rs.getBigDecimal("quantity"),
                rs.getString("unit_code"), rs.getBigDecimal("unit_price_at_order"), rs.getBigDecimal("line_total"),
                OrderStatus.valueOf(rs.getString("order_status"))), farmerId, from(start));
    }

    // ------------------------------------------------------------------ admin

    /** Product-wise report for the whole marketplace. */
    public List<ProductSales> allProducts(LocalDate start) {
        return productSales("", start);
    }

    private List<ProductSales> productSales(String where, LocalDate start, Object... extra) {
        String sql = "SELECT p.product_id, p.product_name, f.farm_name, c.category_name, p.unit_code, "
                   + "       p.available_quantity, p.listing_status, "
                   + "       COUNT(DISTINCT o.order_id) AS orders, "
                   + "       COALESCE(SUM(oi.quantity), 0) AS units, "
                   + "       COALESCE(SUM(oi.line_total), 0) AS revenue "
                   + "FROM product p "
                   + "JOIN farmer_profile f ON f.farmer_id = p.farmer_id "
                   + "JOIN category c ON c.category_id = p.category_id "
                   + "LEFT JOIN (order_item oi "
                   + "           JOIN orders o ON o.order_id = oi.order_id AND " + COUNTED + " AND o.order_date >= ?) "
                   + "       ON oi.product_id = p.product_id "
                   + where
                   + "GROUP BY p.product_id, p.product_name, f.farm_name, c.category_name, p.unit_code, "
                   + "         p.available_quantity, p.listing_status "
                   + "ORDER BY revenue DESC, p.product_name";
        Object[] params = new Object[extra.length + 1];
        params[0] = from(start);
        System.arraycopy(extra, 0, params, 1, extra.length);
        return query(sql, rs -> new ProductSales(rs.getInt("product_id"), rs.getString("product_name"),
                rs.getString("farm_name"), rs.getString("category_name"), rs.getString("unit_code"),
                rs.getInt("orders"), rs.getBigDecimal("units"), rs.getBigDecimal("revenue"),
                rs.getBigDecimal("available_quantity"), ListingStatus.valueOf(rs.getString("listing_status"))), params);
    }

    /** Farmer-wise report: one row per farm, including farms with no sales. */
    public List<FarmerSales> farmers(LocalDate start) {
        String sql = "SELECT f.farmer_id, f.farm_name, u.full_name, f.verification_status, "
                   + "       (SELECT COUNT(*) FROM product p2 WHERE p2.farmer_id = f.farmer_id) AS products, "
                   + "       COUNT(DISTINCT o.order_id) AS orders, "
                   + "       COALESCE(SUM(oi.line_total), 0) AS sales, "
                   + "       COALESCE(SUM(CASE WHEN o.order_status = 'DELIVERED' THEN oi.line_total END), 0) AS delivered "
                   + "FROM farmer_profile f "
                   + "JOIN users u ON u.user_id = f.farmer_id "
                   + "LEFT JOIN product p ON p.farmer_id = f.farmer_id "
                   + "LEFT JOIN (order_item oi "
                   + "           JOIN orders o ON o.order_id = oi.order_id AND " + COUNTED + " AND o.order_date >= ?) "
                   + "       ON oi.product_id = p.product_id "
                   + "GROUP BY f.farmer_id, f.farm_name, u.full_name, f.verification_status "
                   + "ORDER BY sales DESC, f.farm_name";
        return query(sql, rs -> new FarmerSales(rs.getInt("farmer_id"), rs.getString("farm_name"),
                rs.getString("full_name"), Farmer.VerificationStatus.valueOf(rs.getString("verification_status")),
                rs.getInt("products"), rs.getInt("orders"), rs.getBigDecimal("sales"),
                rs.getBigDecimal("delivered")), from(start));
    }

    public List<CategorySales> categories(LocalDate start) {
        String sql = "SELECT c.category_name, "
                   + "       COUNT(DISTINCT p.product_id) AS products, "
                   + "       COUNT(DISTINCT o.order_id) AS orders, "
                   + "       COALESCE(SUM(oi.line_total), 0) AS revenue "
                   + "FROM category c "
                   + "LEFT JOIN product p ON p.category_id = c.category_id "
                   + "LEFT JOIN (order_item oi "
                   + "           JOIN orders o ON o.order_id = oi.order_id AND " + COUNTED + " AND o.order_date >= ?) "
                   + "       ON oi.product_id = p.product_id "
                   + "GROUP BY c.category_name "
                   + "ORDER BY revenue DESC, c.category_name";
        return query(sql, rs -> new CategorySales(rs.getString("category_name"), rs.getInt("products"),
                rs.getInt("orders"), rs.getBigDecimal("revenue")), from(start));
    }
}
