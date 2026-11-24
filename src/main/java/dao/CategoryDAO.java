package dao;

import config.DatabaseConfig;
import exception.DataAccessException;
import exception.DuplicateEntryException;
import model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CategoryDAO {

    public List<Category> findAll() {
        String sql = "SELECT category_id, category_name, description FROM category ORDER BY category_name";
        List<Category> categories = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categories.add(new Category(rs.getInt("category_id"),
                        rs.getString("category_name"), rs.getString("description")));
            }
            return categories;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load categories", e);
        }
    }

    /** Category id -> number of products in it (for the admin screen). */
    public Map<Integer, Integer> countProducts() {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        String sql = "SELECT c.category_id, COUNT(p.product_id) FROM category c "
                   + "LEFT JOIN product p ON p.category_id = c.category_id GROUP BY c.category_id";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getInt(1), rs.getInt(2));
            }
            return counts;
        } catch (SQLException e) {
            throw new DataAccessException("Could not count products per category", e);
        }
    }

    public int insert(String name, String description) {
        String sql = "INSERT INTO category (category_name, description) VALUES (?, ?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setObject(2, description, Types.VARCHAR);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateEntryException("A category called \"" + name + "\" already exists.", e);
        } catch (SQLException e) {
            throw new DataAccessException("Could not add category", e);
        }
    }

    public void update(int categoryId, String name, String description) {
        String sql = "UPDATE category SET category_name = ?, description = ? WHERE category_id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setObject(2, description, Types.VARCHAR);
            ps.setInt(3, categoryId);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateEntryException("A category called \"" + name + "\" already exists.", e);
        } catch (SQLException e) {
            throw new DataAccessException("Could not update category", e);
        }
    }

    /**
     * Deletes an empty category. fk_product_category (ON DELETE RESTRICT)
     * makes the database refuse if any product still uses it.
     *
     * @return false if products still use the category
     */
    public boolean deleteIfUnused(int categoryId) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM category WHERE category_id = ?")) {
            ps.setInt(1, categoryId);
            return ps.executeUpdate() == 1;
        } catch (SQLIntegrityConstraintViolationException e) {
            return false;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete category", e);
        }
    }
}
