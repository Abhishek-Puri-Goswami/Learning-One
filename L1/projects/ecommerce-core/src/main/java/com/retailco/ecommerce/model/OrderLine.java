package com.retailco.ecommerce.model;

import java.math.BigDecimal;

// CONCEPT: Immutable value object (record) -- a frozen copy of a CartItem
// at the moment of checkout. Unlike a CartItem, an OrderLine's price and
// quantity never change afterward, since an order shouldn't change after
// it's placed.
public record OrderLine(String productId, String productName, BigDecimal unitPrice, int quantity) {

    public OrderLine {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("unitPrice must be non-negative: " + unitPrice);
        }
    }

    public static OrderLine fromCartItem(CartItem item) {
        return new OrderLine(item.getProductId(), item.getProductName(), item.getUnitPrice(), item.getQuantity());
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
