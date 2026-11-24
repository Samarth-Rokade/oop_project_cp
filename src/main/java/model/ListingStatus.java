package model;

/** Values match the CHECK constraint on product.listing_status. */
public enum ListingStatus {
    ACTIVE("Active"),
    INACTIVE("Hidden"),
    OUT_OF_STOCK("Out of stock"),
    REMOVED("Removed by admin");

    private final String label;

    ListingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
