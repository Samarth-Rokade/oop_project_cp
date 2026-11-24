package dao;

import config.DatabaseConfig;
import exception.DataAccessException;
import model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
}
