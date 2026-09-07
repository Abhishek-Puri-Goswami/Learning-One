package com.retailco.cartservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// CONCEPT: Request DTO with validation -- @Max here rejects an
// obviously-too-large quantity immediately (400), before it ever reaches
// the service layer.
public class CartItemRequest {

    @NotBlank(message = "productId is required")
    private String productId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    // L1/UC5 boundary hardening: caps a single line at 99 units so a
    // fat-fingered or scripted "large quantity" request (e.g. 999999) is
    // rejected with a clean 400 instead of silently accepted and only
    // failing much later at stock-check/payment time. See
    // edge-cases/edge-case-catalog.md - "Large quantity".
    @Max(value = 99, message = "quantity cannot exceed 99 per line item")
    private Integer quantity;

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
