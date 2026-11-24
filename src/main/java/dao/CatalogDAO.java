package dao;

import config.DatabaseConfig;
import exception.DataAccessException;
import model.FarmSummary;
import model.ListingStatus;
import model.Product;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * What buyers can see: reads from active_product_view, which already
 * hides inactive, out-of-stock and unverified-farmer products.
 */
public class CatalogDAO {

    /**
     * Builds the WHERE clause from whichever filters are set. Values are
     * always bound with ? placeholders.
     */
    public List<Product> search(ProductSearch s) {
        StringBuilder sql = new StringBuilder(
                "SELECT product_id, product_name, description, category_id, category_name, unit_code, "
              + "       available_quantity, price_per_unit, harvest_date, farmer_id, farm_name, farmer_name "
              + "FROM active_product_view WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (s.getText() != null) {
            sql.append(" AND (product_name LIKE ? OR description LIKE ? OR farm_name LIKE ?)");
            String like = "%" + escapeLike(s.getText()) + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (s.getCategoryId() != null) {
            sql.append(" AND category_id = ?");
            params.add(s.getCategoryId());
        }
        if (s.getFarmerId() != null) {
            sql.append(" AND farmer_id = ?");
            params.add(s.getFarmerId());
        }
        if (s.getMinPrice() != null) {
            sql.append(" AND price_per_unit >= ?");
            params.add(s.getMinPrice());
        }
        if (s.getMaxPrice() != null) {
            sql.append(" AND price_per_unit <= ?");
            params.add(s.getMaxPrice());
        }
        sql.append(" ORDER BY ").append(s.getSort().orderBy);

        List<Product> products = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapRow(rs));
                }
            }
            return products;
        } catch (SQLException e) {
            throw new DataAccessException("Could not search products", e);
        }
    }

    /** Farms that currently have something on sale. */
    public List<FarmSummary> findFarmsWithProducts() {
        String sql = "SELECT DISTINCT farmer_id, farm_name FROM active_product_view ORDER BY farm_name";
        List<FarmSummary> farms = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                farms.add(new FarmSummary(rs.getInt("farmer_id"), rs.getString("farm_name")));
            }
            return farms;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load farms", e);
        }
    }

    /** % and _ are wildcards in LIKE; escape them so they match literally. */
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getInt("product_id"));
        p.setProductName(rs.getString("product_name"));
        p.setDescription(rs.getString("description"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setCategoryName(rs.getString("category_name"));
        p.setUnitCode(rs.getString("unit_code"));
        p.setAvailableQuantity(rs.getBigDecimal("available_quantity"));
        p.setPricePerUnit(rs.getBigDecimal("price_per_unit"));
        Date harvest = rs.getDate("harvest_date");
        p.setHarvestDate(harvest == null ? null : harvest.toLocalDate());
        p.setFarmerId(rs.getInt("farmer_id"));
        p.setFarmName(rs.getString("farm_name"));
        p.setFarmerName(rs.getString("farmer_name"));
        p.setListingStatus(ListingStatus.ACTIVE);
        return p;
    }
}
