package service;

import dao.CatalogDAO;
import dao.ProductSearch;
import exception.ValidationException;
import model.FarmSummary;
import model.Product;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Product browsing for buyers. */
public class CatalogService {

    private final CatalogDAO catalogDAO;

    public CatalogService() {
        this(new CatalogDAO());
    }

    public CatalogService(CatalogDAO catalogDAO) {
        this.catalogDAO = catalogDAO;
    }

    public List<Product> search(ProductSearch search) throws ValidationException {
        if (search.getMinPrice() != null && search.getMaxPrice() != null
                && search.getMinPrice().compareTo(search.getMaxPrice()) > 0) {
            throw new ValidationException("Minimum price cannot be more than maximum price.");
        }
        return catalogDAO.search(search);
    }

    public List<FarmSummary> getFarms() {
        return catalogDAO.findFarmsWithProducts();
    }

    /** Everything currently on sale, keyed by product id (used to refresh the cart). */
    public Map<Integer, Product> currentListings() {
        return catalogDAO.search(new ProductSearch()).stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));
    }
}
