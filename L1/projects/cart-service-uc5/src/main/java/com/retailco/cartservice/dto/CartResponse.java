package com.retailco.cartservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class CartResponse {

    private String userId;
    private List<CartItemResponse> items;
    private int itemCount;
    private BigDecimal subtotal;
    private Instant updatedAt;

    public CartResponse() {
    }

    public CartResponse(String userId, List<CartItemResponse> items, int itemCount,
                         BigDecimal subtotal, Instant updatedAt) {
        this.userId = userId;
        this.items = items;
        this.itemCount = itemCount;
        this.subtotal = subtotal;
        this.updatedAt = updatedAt;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<CartItemResponse> getItems() { return items; }
    public void setItems(List<CartItemResponse> items) { this.items = items; }

    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
