package com.retailco.ecommerce.model;

import java.math.BigDecimal;

/**
 * One item on a finished order — basically a frozen photocopy of a
 * {@link CartItem} taken at the moment of checkout. Unlike a cart item,
 * an order line's price and quantity are locked forever once created,
 * because an order shouldn't silently change after it's been placed.
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
