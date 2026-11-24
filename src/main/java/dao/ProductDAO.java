package dao;

import config.DatabaseConfig;
import exception.DataAccessException;
import model.ListingStatus;
import model.Product;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Data access for the product table. */
public class ProductDAO {

    private static final String SELECT_PRODUCT =
            "SELECT p.product_id, p.farmer_id, p.category_id, c.category_name, p.product_name, "
          + "       p.description, p.unit_code, p.available_quantity, p.price_per_unit, "
          + "       p.harvest_date, p.image_path, p.listing_status, p.created_at, p.updated_at "
          + "FROM product p "
          + "JOIN category c ON c.category_id = p.category_id ";

    public List<Product> findByFarmer(int farmerId) {
        String sql = SELECT_PRODUCT + "WHERE p.farmer_id = ? ORDER BY p.updated_at DESC, p.product_id DESC";
        List<Product> products = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, farmerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapRow(rs));
                }
            }
            return products;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load products of farmer #" + farmerId, e);
        }
    }

    public Optional<Product> findById(int productId) {
        String sql = SELECT_PRODUCT + "WHERE p.product_id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load product #" + productId, e);
        }
    }

    /** Inserts the product and returns its generated id. */
    public int insert(Product p) {
        try (Connection con = DatabaseConfig.getConnection()) {
            return insert(con, p);
        } catch (SQLException e) {
            throw new DataAccessException("Could not save product " + p.getProductName(), e);
        }
    }

    /** Insert as part of the caller's transaction (used by CSV import). */
    public int insert(Connection con, Product p) throws SQLException {
        String sql = "INSERT INTO product (farmer_id, category_id, product_name, description, unit_code, "
                   + "available_quantity, price_per_unit, harvest_date, image_path, listing_status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getFarmerId());
            setEditableColumns(ps, p, 2);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("No id returned for new product");
            }
        }
    }

    /**
     * Updates a product. The WHERE clause also checks farmer_id, so a
     * farmer can never change another farmer's row even if the service
     * check were bypassed.
     *
     * @return true if a row was updated
     */
    public boolean update(Product p) {
        String sql = "UPDATE product SET category_id = ?, product_name = ?, description = ?, unit_code = ?, "
                   + "available_quantity = ?, price_per_unit = ?, harvest_date = ?, image_path = ?, "
                   + "listing_status = ? "
                   + "WHERE product_id = ? AND farmer_id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int next = setEditableColumns(ps, p, 1);
            ps.setInt(next, p.getProductId());
            ps.setInt(next + 1, p.getFarmerId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update product #" + p.getProductId(), e);
        }
    }

    /** How many order lines reference this product. */
    public int countOrderItems(int productId) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM order_item WHERE product_id = ?")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check orders of product #" + productId, e);
        }
    }

    /**
     * Deletes a product that has never been ordered. The NOT EXISTS check
     * and the fk_item_product (ON DELETE RESTRICT) foreign key both make
     * sure order history is never lost. Price history rows are removed by
     * ON DELETE CASCADE.
     *
     * @return true if the row was deleted
     */
    public boolean deleteIfNeverOrdered(int productId, int farmerId) {
        String sql = "DELETE FROM product WHERE product_id = ? AND farmer_id = ? "
                   + "AND NOT EXISTS (SELECT 1 FROM order_item oi WHERE oi.product_id = ?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, farmerId);
            ps.setInt(3, productId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete product #" + productId, e);
        }
    }

    /** Binds the columns shared by INSERT and UPDATE; returns the next parameter index. */
    private int setEditableColumns(PreparedStatement ps, Product p, int i) throws SQLException {
        ps.setInt(i++, p.getCategoryId());
        ps.setString(i++, p.getProductName());
        setNullableString(ps, i++, p.getDescription());
        ps.setString(i++, p.getUnitCode());
        ps.setBigDecimal(i++, p.getAvailableQuantity());
        ps.setBigDecimal(i++, p.getPricePerUnit());
        if (p.getHarvestDate() == null) {
            ps.setNull(i++, Types.DATE);
        } else {
            ps.setDate(i++, Date.valueOf(p.getHarvestDate()));
        }
        setNullableString(ps, i++, p.getImagePath());
        ps.setString(i++, p.getListingStatus().name());
        return i;
    }

    private static void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value.trim());
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getInt("product_id"));
        p.setFarmerId(rs.getInt("farmer_id"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setCategoryName(rs.getString("category_name"));
        p.setProductName(rs.getString("product_name"));
        p.setDescription(rs.getString("description"));
        p.setUnitCode(rs.getString("unit_code"));
        p.setAvailableQuantity(rs.getBigDecimal("available_quantity"));
        p.setPricePerUnit(rs.getBigDecimal("price_per_unit"));
        Date harvest = rs.getDate("harvest_date");
        p.setHarvestDate(harvest == null ? null : harvest.toLocalDate());
        p.setImagePath(rs.getString("image_path"));
        p.setListingStatus(ListingStatus.valueOf(rs.getString("listing_status")));
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        p.setCreatedAt(created == null ? null : created.toLocalDateTime());
        p.setUpdatedAt(updated == null ? null : updated.toLocalDateTime());
        return p;
    }
}
