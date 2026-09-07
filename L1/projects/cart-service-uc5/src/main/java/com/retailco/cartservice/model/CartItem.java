package com.retailco.cartservice.model;

import java.math.BigDecimal;

/**
 * One line inside a shopping cart — a product, a quantity, and the price
 * it had when added. Notice {@code getLineTotal()} calculates
 * {@code unitPrice * quantity} fresh every time it's called, instead of
 * storing the total as its own field. That way it's never possible for
 * the stored total to fall out of sync with the actual price/quantity.
 */
public class CartItem {

    private String itemId;
    private String productId;
    private String productName;
    private BigDecimal unitPrice;
    private int quantity;

    public CartItem() {
    }

    public CartItem(String itemId, String productId, String productName, BigDecimal unitPrice, int quantity) {
        this.itemId = itemId;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
