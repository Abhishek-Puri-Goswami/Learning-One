package com.retailco.cartservice.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One user's shopping cart — a list of {@link CartItem} lines. Keep in
 * mind that a cart only holds a snapshot of price and product info; it's
 * not the live, always-current source of truth for that data (the
 * product catalog is). See {@code CartServiceImpl} for where the current
 * price actually gets looked up.
 */
public class Cart {

    private String userId;
    private List<CartItem> items = new ArrayList<>();
    private Instant updatedAt;

    public Cart() {
    }

    public Cart(String userId) {
        this.userId = userId;
        this.updatedAt = Instant.now();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
