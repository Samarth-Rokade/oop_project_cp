package service;

import dao.PriceHistoryDAO;
import dao.ProductDAO;
import exception.ValidationException;
import interfaces.ProductOperations;
import model.Farmer;
import model.ListingStatus;
import model.PriceHistory;
import model.Product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.StringJoiner;

/**
 * Business rules for a farmer's product listings.
 *
 * Rules owned here (Java layer):
 *   - a farmer may only see/modify their own products
 *   - input validation (name, price, quantity, dates, lengths)
 *   - status follows stock: ACTIVE <-> OUT_OF_STOCK
 *   - products REMOVED by the admin cannot be edited or re-activated
 * Rules owned by the database: price history (trigger), quantity >= 0 and
 * price > 0 (CHECK constraints) as a final safety net.
 */
public class ProductService implements ProductOperations {

    /** Largest value that fits DECIMAL(10,2). */
    static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");

    private final ProductDAO productDAO;
    private final PriceHistoryDAO priceHistoryDAO;

    public ProductService() {
        this(new ProductDAO(), new PriceHistoryDAO());
    }

    public ProductService(ProductDAO productDAO, PriceHistoryDAO priceHistoryDAO) {
        this.productDAO = productDAO;
        this.priceHistoryDAO = priceHistoryDAO;
    }

    @Override
    public List<Product> getProductsOf(Farmer farmer) {
        return productDAO.findByFarmer(farmer.getUserId());
    }

    @Override
    public Product addProduct(Farmer farmer, Product product) throws ValidationException {
        Product toSave = new Product(product);
        toSave.setFarmerId(farmer.getUserId());
        if (toSave.getListingStatus() == null || toSave.getListingStatus() == ListingStatus.REMOVED) {
            toSave.setListingStatus(ListingStatus.ACTIVE);
        }
        normalise(toSave);
        validate(toSave);
        toSave.applyStockRule();

        int id = productDAO.insert(toSave);
        return reload(id);
    }

    @Override
    public Product updateProduct(Farmer farmer, Product product) throws ValidationException {
        Product existing = loadOwned(farmer, product.getProductId());

        Product toSave = new Product(product);
        toSave.setFarmerId(farmer.getUserId());
        // Farmers choose only between ACTIVE and hidden; stock decides OUT_OF_STOCK.
        if (toSave.getListingStatus() == ListingStatus.REMOVED) {
            toSave.setListingStatus(existing.getListingStatus());
        }
        normalise(toSave);
        validate(toSave);
        toSave.applyStockRule();

        save(toSave);
        return reload(toSave.getProductId());
    }

    @Override
    public Product updateStock(Farmer farmer, int productId, BigDecimal newQuantity) throws ValidationException {
        Product product = loadOwned(farmer, productId);
        product.setAvailableQuantity(newQuantity);
        validate(product);
        product.applyStockRule();
        save(product);
        return reload(productId);
    }

    @Override
    public Product setAvailability(Farmer farmer, int productId, boolean available) throws ValidationException {
        Product product = loadOwned(farmer, productId);
        product.setListingStatus(available ? ListingStatus.ACTIVE : ListingStatus.INACTIVE);
        product.applyStockRule();
        save(product);
        return reload(productId);
    }

    @Override
    public List<PriceHistory> getPriceHistory(Farmer farmer, int productId) throws ValidationException {
        loadOwned(farmer, productId);
        return priceHistoryDAO.findByProduct(productId);
    }

    // ------------------------------------------------------------------

    /**
     * Checks every input rule and reports all problems at once, so the
     * user can fix the whole form in one go.
     */
    public static void validate(Product p) throws ValidationException {
        StringJoiner errors = new StringJoiner("\n");

        String name = p.getProductName();
        if (name == null || name.isBlank()) {
            errors.add("Product name is required.");
        } else if (name.trim().length() > 100) {
            errors.add("Product name must be at most 100 characters.");
        }
        if (p.getCategoryId() <= 0) {
            errors.add("Choose a category.");
        }
        if (p.getUnitCode() == null || p.getUnitCode().isBlank()) {
            errors.add("Choose a unit.");
        }

        BigDecimal qty = p.getAvailableQuantity();
        if (qty == null) {
            errors.add("Quantity is required.");
        } else if (qty.signum() < 0) {
            errors.add("Quantity cannot be negative.");
        } else if (qty.compareTo(MAX_AMOUNT) > 0) {
            errors.add("Quantity is too large.");
        } else if (qty.stripTrailingZeros().scale() > 2) {
            errors.add("Quantity can have at most 2 decimal places.");
        }

        BigDecimal price = p.getPricePerUnit();
        if (price == null) {
            errors.add("Price is required.");
        } else if (price.signum() <= 0) {
            errors.add("Price must be greater than zero.");
        } else if (price.compareTo(MAX_AMOUNT) > 0) {
            errors.add("Price is too large.");
        } else if (price.stripTrailingZeros().scale() > 2) {
            errors.add("Price can have at most 2 decimal places.");
        }

        if (p.getHarvestDate() != null && p.getHarvestDate().isAfter(LocalDate.now())) {
            errors.add("Harvest date cannot be in the future.");
        }
        if (p.getDescription() != null && p.getDescription().length() > 500) {
            errors.add("Description must be at most 500 characters.");
        }
        if (p.getImagePath() != null && p.getImagePath().length() > 255) {
            errors.add("Image path must be at most 255 characters.");
        }

        if (errors.length() > 0) {
            throw new ValidationException(errors.toString());
        }
    }

    private static void normalise(Product p) {
        if (p.getProductName() != null) {
            p.setProductName(p.getProductName().trim());
        }
        if (p.getDescription() != null) {
            String d = p.getDescription().trim();
            p.setDescription(d.isEmpty() ? null : d);
        }
        if (p.getImagePath() != null) {
            String i = p.getImagePath().trim();
            p.setImagePath(i.isEmpty() ? null : i);
        }
    }

    /** Loads a product and makes sure it belongs to this farmer and is editable. */
    private Product loadOwned(Farmer farmer, int productId) throws ValidationException {
        Product product = productDAO.findById(productId)
                .filter(p -> p.getFarmerId() == farmer.getUserId())
                .orElseThrow(() -> new ValidationException("Product not found in your listings."));
        if (product.getListingStatus() == ListingStatus.REMOVED) {
            throw new ValidationException("This listing was removed by the administrator and cannot be changed.");
        }
        return product;
    }

    private void save(Product product) throws ValidationException {
        if (!productDAO.update(product)) {
            throw new ValidationException("Product not found in your listings.");
        }
    }

    private Product reload(int productId) throws ValidationException {
        return productDAO.findById(productId)
                .orElseThrow(() -> new ValidationException("Product #" + productId + " no longer exists."));
    }
}
