package dao;

import config.DatabaseConfig;
import exception.DataAccessException;
import model.Address;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AddressDAO {

    public List<Address> findByUser(int userId) {
        String sql = "SELECT address_id, user_id, address_line, city, district, state, postal_code, is_default "
                   + "FROM address WHERE user_id = ? ORDER BY is_default DESC, address_id";
        List<Address> addresses = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    addresses.add(new Address(
                            rs.getInt("address_id"), rs.getInt("user_id"), rs.getString("address_line"),
                            rs.getString("city"), rs.getString("district"), rs.getString("state"),
                            rs.getString("postal_code"), rs.getBoolean("is_default")));
                }
            }
            return addresses;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load addresses", e);
        }
    }

    /**
     * Inserts an address. If it is the new default, the user's other
     * addresses lose their default flag in the same transaction.
     */
    public int insert(Address a) {
        String clearDefault = "UPDATE address SET is_default = FALSE WHERE user_id = ?";
        String insert = "INSERT INTO address (user_id, address_line, city, district, state, postal_code, is_default) "
                      + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (a.isDefaultAddress()) {
                    try (PreparedStatement ps = con.prepareStatement(clearDefault)) {
                        ps.setInt(1, a.getUserId());
                        ps.executeUpdate();
                    }
                }
                int id;
                try (PreparedStatement ps = con.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, a.getUserId());
                    ps.setString(2, a.getAddressLine());
                    ps.setString(3, a.getCity());
                    ps.setString(4, a.getDistrict());
                    ps.setString(5, a.getState());
                    ps.setString(6, a.getPostalCode());
                    ps.setBoolean(7, a.isDefaultAddress());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        id = keys.getInt(1);
                    }
                }
                con.commit();
                return id;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save address", e);
        }
    }
}
