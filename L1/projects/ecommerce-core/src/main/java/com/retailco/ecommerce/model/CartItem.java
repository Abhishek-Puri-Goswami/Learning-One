package com.retailco.ecommerce.model;

import java.math.BigDecimal;

/**
 * Mirrors L1/UC2 {@code cart-service}'s existing {@code CartItem} POJO
 * (itemId, productId, productName, unitPrice, quantity) including its
 * {@code getLineTotal()} calculation. As in the original, {@code unitPrice}
 * is a price *snapshot* taken when the item was added -- this class is not
 * the system of record for current product price/stock (that's
 * {@link com.retailco.ecommerce.catalog.ProductCatalog}), matching the
 * comment already present on the source {@code Cart} model.
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
