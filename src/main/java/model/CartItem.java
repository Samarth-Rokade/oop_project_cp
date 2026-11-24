package model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A product and the quantity the buyer wants. */
public class CartItem {

    private final Product product;
    private BigDecimal quantity;

    public CartItem(Product product, BigDecimal quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    /** Estimate shown in the cart; the real total is recalculated from database prices. */
    public BigDecimal getLineTotal() {
        return product.getPricePerUnit().multiply(quantity).setScale(2, RoundingMode.HALF_UP);
    }
}
