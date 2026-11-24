package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** One price change of a product. Rows are written by a database trigger. */
public class PriceHistory {

    private final int priceHistoryId;
    private final int productId;
    private final BigDecimal oldPrice;
    private final BigDecimal newPrice;
    private final LocalDateTime changedAt;
    private final String changedByName;

    public PriceHistory(int priceHistoryId, int productId, BigDecimal oldPrice, BigDecimal newPrice,
                        LocalDateTime changedAt, String changedByName) {
        this.priceHistoryId = priceHistoryId;
        this.productId = productId;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.changedAt = changedAt;
        this.changedByName = changedByName;
    }

    /** Percentage change from the old price, e.g. 7.69 for +7.69 %. */
    public BigDecimal getChangePercent() {
        if (oldPrice == null || oldPrice.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return newPrice.subtract(oldPrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(oldPrice, 2, RoundingMode.HALF_UP);
    }

    public int getPriceHistoryId() {
        return priceHistoryId;
    }

    public int getProductId() {
        return productId;
    }

    public BigDecimal getOldPrice() {
        return oldPrice;
    }

    public BigDecimal getNewPrice() {
        return newPrice;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public String getChangedByName() {
        return changedByName;
    }
}
