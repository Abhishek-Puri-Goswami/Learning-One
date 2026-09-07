package com.retailco.cartservice.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// CONCEPT: Domain model -- one user's cart (a list of CartItem lines).
// IMPORTANT: a Cart is only a snapshot of price/product info, not the
// live source of truth (that's the product catalog) -- see
// CartServiceImpl for where price is re-fetched.
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
