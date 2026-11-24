package dao;

import config.DatabaseConfig;
import exception.DataAccessException;
import exception.DuplicateEntryException;
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
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Types;
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

    public boolean emailExists(String email) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT 1 FROM users WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check email", e);
        }
    }

    /**
     * Creates a farmer: one row in users and one in farmer_profile, in a
     * single transaction (both or neither). New farmers wait for the
     * administrator: account PENDING, verification PENDING.
     *
     * @return the new user id
     */
    public int insertFarmer(String fullName, String email, String phone, String passwordHash,
                            String farmName, String farmAddress) {
        String profileSql = "INSERT INTO farmer_profile (farmer_id, farm_name, farm_address, verification_status) "
                          + "VALUES (?, ?, ?, 'PENDING')";
        return insertUser(fullName, email, phone, passwordHash, Role.FARMER, AccountStatus.PENDING, (con, id) -> {
            try (PreparedStatement ps = con.prepareStatement(profileSql)) {
                ps.setInt(1, id);
                ps.setString(2, farmName);
                ps.setString(3, farmAddress);
                ps.executeUpdate();
            }
        });
    }

    /** Creates a buyer (users + buyer_profile) in one transaction. Buyers are active immediately. */
    public int insertBuyer(String fullName, String email, String phone, String passwordHash, Buyer.BuyerType type) {
        String profileSql = "INSERT INTO buyer_profile (buyer_id, buyer_type) VALUES (?, ?)";
        return insertUser(fullName, email, phone, passwordHash, Role.BUYER, AccountStatus.ACTIVE, (con, id) -> {
            try (PreparedStatement ps = con.prepareStatement(profileSql)) {
                ps.setInt(1, id);
                ps.setString(2, type.name());
                ps.executeUpdate();
            }
        });
    }

    /**
     * Updates a farmer's own details (users + farmer_profile) in one transaction.
     * Email, role, account and verification status are not changed here.
     */
    public void updateFarmerProfile(int farmerId, String fullName, String phone,
                                    String farmName, String farmAddress) {
        String profileSql = "UPDATE farmer_profile SET farm_name = ?, farm_address = ? WHERE farmer_id = ?";
        updateUser(farmerId, fullName, phone, con -> {
            try (PreparedStatement ps = con.prepareStatement(profileSql)) {
                ps.setString(1, farmName);
                ps.setString(2, farmAddress);
                ps.setInt(3, farmerId);
                ps.executeUpdate();
            }
        });
    }

    /** Updates a buyer's own details (users + buyer_profile) in one transaction. */
    public void updateBuyerProfile(int buyerId, String fullName, String phone, Buyer.BuyerType type) {
        String profileSql = "UPDATE buyer_profile SET buyer_type = ? WHERE buyer_id = ?";
        updateUser(buyerId, fullName, phone, con -> {
            try (PreparedStatement ps = con.prepareStatement(profileSql)) {
                ps.setString(1, type.name());
                ps.setInt(2, buyerId);
                ps.executeUpdate();
            }
        });
    }

    /** Updates a role's profile table inside the caller's transaction. */
    @FunctionalInterface
    private interface ProfileUpdate {
        void update(Connection con) throws SQLException;
    }

    private void updateUser(int userId, String fullName, String phone, ProfileUpdate profile) {
        String sql = "UPDATE users SET full_name = ?, phone = ? WHERE user_id = ?";
        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, fullName);
                    ps.setObject(2, phone, Types.VARCHAR);
                    ps.setInt(3, userId);
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("User #" + userId + " no longer exists");
                    }
                }
                profile.update(con);
                new AuditDAO().log(con, userId, "UPDATE_PROFILE", "USER", userId, "Profile details updated");
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not update the profile", e);
        }
    }

    /** Inserts into a role's profile table, using the new user id. */
    @FunctionalInterface
    private interface ProfileInsert {
        void insert(Connection con, int userId) throws SQLException;
    }

    private int insertUser(String fullName, String email, String phone, String passwordHash,
                           Role role, AccountStatus status, ProfileInsert profile) {
        String sql = "INSERT INTO users (full_name, email, phone, password_hash, role, account_status) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                int userId;
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, fullName);
                    ps.setString(2, email);
                    ps.setObject(3, phone, Types.VARCHAR);
                    ps.setString(4, passwordHash);
                    ps.setString(5, role.name());
                    ps.setString(6, status.name());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        userId = keys.getInt(1);
                    }
                }
                profile.insert(con, userId);
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO audit_log (user_id, action, entity_type, entity_id, details) "
                      + "VALUES (?, 'REGISTER', 'USER', ?, ?)")) {
                    ps.setInt(1, userId);
                    ps.setInt(2, userId);
                    ps.setString(3, "New " + role.name().toLowerCase() + " account");
                    ps.executeUpdate();
                }
                con.commit();
                return userId;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            // uq_users_email: someone registered this email first
            throw new DuplicateEntryException("This email is already registered.", e);
        } catch (SQLException e) {
            throw new DataAccessException("Could not create the account", e);
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
