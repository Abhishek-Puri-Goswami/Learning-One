package com.retailco.ecommerce.model;

import java.math.BigDecimal;

/**
 * One line inside a shopping cart: a single product plus how many of it
 * the customer wants. The {@code unitPrice} field is a snapshot of the
 * price at the moment this item was added — see {@link Cart}'s comment
 * for why that matters.
 */
public class CartItem {

    private final String itemId;
    private final String productId;
    private String productName;
    private BigDecimal unitPrice;
    private int quantity;

    public CartItem(String itemId, String productId, String productName,
                     BigDecimal unitPrice, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("unitPrice must be non-negative: " + unitPrice);
        }
        this.itemId = itemId;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public String getItemId() { return itemId; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        this.quantity = quantity;
    }

    /** Unchanged from the L1/UC2 source: unitPrice * quantity. */
    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public String toString() {
        return "CartItem[itemId=" + itemId + ", productId=" + productId
                + ", quantity=" + quantity + ", lineTotal=" + getLineTotal() + "]";
    }
}
