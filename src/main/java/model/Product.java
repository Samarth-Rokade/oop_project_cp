package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A product listed by a farmer. Quantities and prices use BigDecimal
 * because they map to DECIMAL columns and must not lose precision.
 */
public class Product {

    private int productId;
    private int farmerId;
    private int categoryId;
    private String categoryName;
    private String productName;
    private String description;
    private String unitCode;
    private BigDecimal availableQuantity = BigDecimal.ZERO;
    private BigDecimal pricePerUnit;
    private LocalDate harvestDate;
    private String imagePath;
    private ListingStatus listingStatus = ListingStatus.ACTIVE;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** Seller info - filled when products are loaded for buyers. */
    private String farmName;
    private String farmerName;

    public Product() {
    }

    /** Copy constructor: lets a form edit a copy without touching the original. */
    public Product(Product other) {
        this.productId = other.productId;
        this.farmerId = other.farmerId;
        this.categoryId = other.categoryId;
        this.categoryName = other.categoryName;
        this.productName = other.productName;
        this.description = other.description;
        this.unitCode = other.unitCode;
        this.availableQuantity = other.availableQuantity;
        this.pricePerUnit = other.pricePerUnit;
        this.harvestDate = other.harvestDate;
        this.imagePath = other.imagePath;
        this.listingStatus = other.listingStatus;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
        this.farmName = other.farmName;
        this.farmerName = other.farmerName;
    }

    /**
     * Keeps the status consistent with stock:
     * ACTIVE with zero stock becomes OUT_OF_STOCK, and OUT_OF_STOCK with
     * stock again becomes ACTIVE. Hidden/removed listings are untouched.
     */
    public void applyStockRule() {
        boolean empty = availableQuantity == null || availableQuantity.signum() <= 0;
        if (listingStatus == ListingStatus.ACTIVE && empty) {
            listingStatus = ListingStatus.OUT_OF_STOCK;
        } else if (listingStatus == ListingStatus.OUT_OF_STOCK && !empty) {
            listingStatus = ListingStatus.ACTIVE;
        }
    }

    public boolean isVisibleToBuyers() {
        return listingStatus == ListingStatus.ACTIVE
                && availableQuantity != null && availableQuantity.signum() > 0;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(int farmerId) {
        this.farmerId = farmerId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public BigDecimal getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(BigDecimal availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(BigDecimal pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    public LocalDate getHarvestDate() {
        return harvestDate;
    }

    public void setHarvestDate(LocalDate harvestDate) {
        this.harvestDate = harvestDate;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public ListingStatus getListingStatus() {
        return listingStatus;
    }

    public void setListingStatus(ListingStatus listingStatus) {
        this.listingStatus = listingStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFarmName() {
        return farmName;
    }

    public void setFarmName(String farmName) {
        this.farmName = farmName;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }

    @Override
    public String toString() {
        return productName + " (#" + productId + ")";
    }
}
