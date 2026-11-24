package dao;

import config.DatabaseConfig;
import exception.DataAccessException;
import model.PriceHistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Read-only: price_history rows are inserted by trg_product_price_history. */
public class PriceHistoryDAO {

    public List<PriceHistory> findByProduct(int productId) {
        String sql = "SELECT ph.price_history_id, ph.product_id, ph.old_price, ph.new_price, "
                   + "       ph.changed_at, u.full_name "
                   + "FROM price_history ph "
                   + "LEFT JOIN users u ON u.user_id = ph.changed_by "
                   + "WHERE ph.product_id = ? "
                   + "ORDER BY ph.changed_at DESC, ph.price_history_id DESC";
        List<PriceHistory> history = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(new PriceHistory(
                            rs.getInt("price_history_id"),
                            rs.getInt("product_id"),
                            rs.getBigDecimal("old_price"),
                            rs.getBigDecimal("new_price"),
                            rs.getTimestamp("changed_at").toLocalDateTime(),
                            rs.getString("full_name")));
                }
            }
            return history;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load price history of product #" + productId, e);
        }
    }
}
