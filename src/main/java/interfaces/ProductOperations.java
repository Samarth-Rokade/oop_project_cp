package interfaces;

import exception.ValidationException;
import model.Farmer;
import model.PriceHistory;
import model.Product;

import java.math.BigDecimal;
import java.util.List;

/**
 * What a farmer can do with their product listings (spec 9.5).
 * Also shows a default method and a static method in an interface (spec 9.14).
 */
public interface ProductOperations {

    /** Stock below this (but above zero) counts as "low". */
    BigDecimal LOW_STOCK_THRESHOLD = BigDecimal.TEN;

    List<Product> getProductsOf(Farmer farmer);

    Product addProduct(Farmer farmer, Product product) throws ValidationException;

    Product updateProduct(Farmer farmer, Product product) throws ValidationException;

    Product updateStock(Farmer farmer, int productId, BigDecimal newQuantity) throws ValidationException;

    Product setAvailability(Farmer farmer, int productId, boolean available) throws ValidationException;

    List<PriceHistory> getPriceHistory(Farmer farmer, int productId) throws ValidationException;

    /** Default method: shared behaviour every implementation gets for free. */
    default boolean isLowStock(Product product) {
        return isLowStock(product.getAvailableQuantity());
    }

    /** Static method: utility that belongs to the interface itself. */
    static boolean isLowStock(BigDecimal quantity) {
        return quantity != null
                && quantity.signum() > 0
                && quantity.compareTo(LOW_STOCK_THRESHOLD) < 0;
    }
}
