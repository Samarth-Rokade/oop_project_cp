package model;

import java.math.BigDecimal;

/**
 * One line of an order. unitPriceAtOrder is the price when the order was
 * placed; it never changes even if the product price changes later.
 */
public class OrderItem {

    private int orderItemId;
    private int orderId;
    private int productId;
    private String productName;
    private String unitCode;
    private String farmName;
    private BigDecimal quantity;
    private BigDecimal unitPriceAtOrder;
    private BigDecimal lineTotal;

    public int getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(int orderItemId) {
        this.orderItemId = orderItemId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public String getFarmName() {
        return farmName;
    }

    public void setFarmName(String farmName) {
        this.farmName = farmName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPriceAtOrder() {
        return unitPriceAtOrder;
    }

    public void setUnitPriceAtOrder(BigDecimal unitPriceAtOrder) {
        this.unitPriceAtOrder = unitPriceAtOrder;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
