package com.retailco.cartservice.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Cart is intentionally NOT the system of record for price/stock (see L1/UC1
 * ADR + architecture.json risk: "Stale price/stock shown in cart"). Backed by
 * an in-memory map here; production target is Redis per ADR-002.
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
