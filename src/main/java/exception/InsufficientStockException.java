package exception;

import java.math.BigDecimal;

/** A buyer asked for more than the farmer has in stock (spec 9.10). */
public class InsufficientStockException extends ValidationException {

    private final int productId;
    private final BigDecimal available;

    public InsufficientStockException(int productId, String productName, BigDecimal requested,
                                      BigDecimal available, String unit) {
        super("Not enough stock for " + productName + ": you asked for "
                + requested.stripTrailingZeros().toPlainString() + " " + unit + " but only "
                + available.stripTrailingZeros().toPlainString() + " " + unit + " is available.");
        this.productId = productId;
        this.available = available;
    }

    public int getProductId() {
        return productId;
    }

    public BigDecimal getAvailable() {
        return available;
    }
}
