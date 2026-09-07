package com.retailco.ecommerce.model;

import java.math.BigDecimal;

/**
 * A committed line item on an {@link Order} -- a frozen copy of the
 * {@link CartItem} it came from (price and quantity locked at checkout
 * time, immutable from here on, unlike a cart line which can still change).
 */
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
