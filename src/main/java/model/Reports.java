package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Report rows. "Sales" means order lines whose order was not rejected,
 * cancelled or failed; "delivered" counts only completed orders.
 */
public final class Reports {

    private Reports() {
    }

    /** Headline numbers for a farmer's Sales page. */
    public record SalesSummary(int orders, BigDecimal salesValue, BigDecimal deliveredValue) {

        public BigDecimal averageOrder() {
            return orders == 0 ? BigDecimal.ZERO
                    : salesValue.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP);
        }
    }

    /** One product's performance (product-wise report). */
    public record ProductSales(int productId, String productName, String farmName, String categoryName,
                               String unitCode, int orders, BigDecimal unitsSold, BigDecimal revenue,
                               BigDecimal stock, ListingStatus status) {
    }

    /** One farm's totals (farmer-wise report). */
    public record FarmerSales(int farmerId, String farmName, String farmerName, Farmer.VerificationStatus verification,
                              int products, int orders, BigDecimal salesValue, BigDecimal deliveredValue) {
    }

    /** One category's totals. */
    public record CategorySales(String categoryName, int products, int orders, BigDecimal revenue) {
    }

    /** One order line in a farmer's sales history. */
    public record SaleLine(int orderId, LocalDateTime orderDate, String buyerName, String productName,
                           BigDecimal quantity, String unitCode, BigDecimal unitPrice, BigDecimal lineTotal,
                           OrderStatus status) {
    }
}
