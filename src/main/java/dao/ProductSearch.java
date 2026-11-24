package dao;

import java.math.BigDecimal;

/**
 * Filters for the buyer's product search. Every field is optional
 * (null = no filter). Sorting uses a fixed enum, so user input can
 * never be pasted into the ORDER BY clause.
 */
public class ProductSearch {

    public enum Sort {
        NAME("Name (A-Z)", "product_name ASC"),
        PRICE_LOW("Price: low to high", "price_per_unit ASC, product_name ASC"),
        PRICE_HIGH("Price: high to low", "price_per_unit DESC, product_name ASC"),
        FRESHEST("Freshest harvest", "harvest_date DESC, product_name ASC");

        private final String label;
        final String orderBy;

        Sort(String label, String orderBy) {
            this.label = label;
            this.orderBy = orderBy;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private String text;
    private Integer categoryId;
    private Integer farmerId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Sort sort = Sort.NAME;

    public String getText() {
        return text;
    }

    public ProductSearch text(String text) {
        this.text = text == null || text.isBlank() ? null : text.trim();
        return this;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public ProductSearch categoryId(Integer categoryId) {
        this.categoryId = categoryId;
        return this;
    }

    public Integer getFarmerId() {
        return farmerId;
    }

    public ProductSearch farmerId(Integer farmerId) {
        this.farmerId = farmerId;
        return this;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public ProductSearch minPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
        return this;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public ProductSearch maxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
        return this;
    }

    public Sort getSort() {
        return sort;
    }

    public ProductSearch sort(Sort sort) {
        this.sort = sort == null ? Sort.NAME : sort;
        return this;
    }
}
