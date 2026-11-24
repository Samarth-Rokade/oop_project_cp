package dao;

import config.DatabaseConfig;
import exception.DataAccessException;
import model.AccountStatus;
import model.Admin;
import model.Buyer;
import model.Farmer;
import model.Role;
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Data access for users. All SQL uses PreparedStatement (no string
 * concatenation) to prevent SQL injection.
 */
public class UserDAO {

    private static final String SELECT_USER =
            "SELECT u.user_id, u.full_name, u.email, u.phone, u.password_hash, u.role, "
          + "       u.account_status, u.created_at, "
          + "       f.farm_name, f.farm_address, f.verification_status, b.buyer_type "
          + "FROM users u "
          + "LEFT JOIN farmer_profile f ON f.farmer_id = u.user_id "
          + "LEFT JOIN buyer_profile  b ON b.buyer_id  = u.user_id ";

    public Optional<User> findByEmail(String email) {
        String sql = SELECT_USER + "WHERE u.email = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load user " + email, e);
        }
    }

    public Optional<User> findById(int userId) {
        String sql = SELECT_USER + "WHERE u.user_id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load user #" + userId, e);
        }
    }

    /** Builds the right subclass for the row's role. */
    private User mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("user_id");
        String name = rs.getString("full_name");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        String hash = rs.getString("password_hash");
        AccountStatus status = AccountStatus.valueOf(rs.getString("account_status"));
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

        return switch (Role.valueOf(rs.getString("role"))) {
            case FARMER -> new Farmer(id, name, email, phone, hash, status, createdAt,
                    rs.getString("farm_name"),
                    rs.getString("farm_address"),
                    Farmer.VerificationStatus.valueOf(rs.getString("verification_status")));
            case BUYER -> new Buyer(id, name, email, phone, hash, status, createdAt,
                    Buyer.BuyerType.valueOf(rs.getString("buyer_type")));
            case ADMIN -> new Admin(id, name, email, phone, hash, status, createdAt);
        };
    }
}
