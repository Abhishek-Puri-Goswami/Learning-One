package com.retailco.cartservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * What a caller must send us to add an item to the cart. The
 * {@code @Max(99)} rule below rejects an unreasonably large quantity
 * (imagine a typo like 999999, or a script gone wrong) immediately, with
 * a clean 400 error — instead of silently accepting it and only
 * discovering the problem much later, when we try to check stock or take
 * payment.
 */
public class CartItemRequest {

    @NotBlank(message = "productId is required")
    private String productId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    @Max(value = 99, message = "quantity cannot exceed 99 per line item")
    private Integer quantity;

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
