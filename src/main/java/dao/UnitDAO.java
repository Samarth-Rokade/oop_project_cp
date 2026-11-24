package dao;

import config.DatabaseConfig;
import exception.DataAccessException;
import exception.DuplicateEntryException;
import model.Unit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UnitDAO {

    public List<Unit> findAll() {
        String sql = "SELECT unit_code, unit_name FROM unit ORDER BY unit_name";
        List<Unit> units = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                units.add(new Unit(rs.getString("unit_code"), rs.getString("unit_name")));
            }
            return units;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load units", e);
        }
    }

    /** Unit code -> number of products sold in that unit. */
    public Map<String, Integer> countProducts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        String sql = "SELECT u.unit_code, COUNT(p.product_id) FROM unit u "
                   + "LEFT JOIN product p ON p.unit_code = u.unit_code GROUP BY u.unit_code";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getString(1), rs.getInt(2));
            }
            return counts;
        } catch (SQLException e) {
            throw new DataAccessException("Could not count products per unit", e);
        }
    }

    public void insert(String code, String name) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO unit (unit_code, unit_name) VALUES (?, ?)")) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateEntryException("Unit code " + code + " already exists.", e);
        } catch (SQLException e) {
            throw new DataAccessException("Could not add unit", e);
        }
    }

    public void rename(String code, String name) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE unit SET unit_name = ? WHERE unit_code = ?")) {
            ps.setString(1, name);
            ps.setString(2, code);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not rename unit", e);
        }
    }

    /** @return false if products still use the unit (fk_product_unit refuses the delete) */
    public boolean deleteIfUnused(String code) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM unit WHERE unit_code = ?")) {
            ps.setString(1, code);
            return ps.executeUpdate() == 1;
        } catch (SQLIntegrityConstraintViolationException e) {
            return false;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete unit", e);
        }
    }
}
