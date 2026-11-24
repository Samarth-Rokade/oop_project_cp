package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Writes rows to audit_log. Status changes are already logged by the
 * trg_orders_status_audit trigger; this is for extra details such as
 * the reason an order was rejected.
 */
public class AuditDAO {

    public void log(Connection con, Integer userId, String action, String entityType, Integer entityId,
                    String details) throws SQLException {
        String sql = "INSERT INTO audit_log (user_id, action, entity_type, entity_id, details) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, userId, Types.INTEGER);
            ps.setString(2, action);
            ps.setString(3, entityType);
            ps.setObject(4, entityId, Types.INTEGER);
            ps.setString(5, details == null || details.length() <= 500 ? details : details.substring(0, 500));
            ps.executeUpdate();
        }
    }
}
