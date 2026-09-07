package com.retailco.ecommerce.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mirrors L1/UC2 {@code cart-service}'s existing {@code Cart} POJO
 * (userId, items, updatedAt). As documented on that original class, a
 * {@code Cart} is not the system of record for price/stock -- it only
 * holds a snapshot taken at add-to-cart time; {@link com.retailco.ecommerce.catalog.ProductCatalog}
 * is authoritative, and {@link com.retailco.ecommerce.order.CheckoutService}
 * re-validates against it before committing an order.
 */
public class Cart {

    private final String userId;
    private final List<CartItem> items = new ArrayList<>();
    private Instant updatedAt;

    public Cart(String userId, Instant updatedAt) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        this.userId = userId;
        this.updatedAt = updatedAt;
    }

    public String getUserId() { return userId; }
    public List<CartItem> getItems() { return List.copyOf(items); }
    public Instant getUpdatedAt() { return updatedAt; }

    public Optional<CartItem> findByProductId(String productId) {
        return items.stream().filter(i -> i.getProductId().equals(productId)).findFirst();
    }

    public void addOrMergeItem(CartItem item, Instant now) {
        Optional<CartItem> existing = findByProductId(item.getProductId());
        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + item.getQuantity());
        } else {
            items.add(item);
        }
        this.updatedAt = now;
    }

    public boolean removeItem(String productId, Instant now) {
        boolean removed = items.removeIf(i -> i.getProductId().equals(productId));
        if (removed) {
            this.updatedAt = now;
        }
        return removed;
    }

    /**
     * Looks up a line by its own generated {@code itemId} rather than by
     * {@code productId} -- needed by callers (e.g. L1/UC2's cart-service
     * REST contract) whose update/delete endpoints are addressed by the
     * cart *line*, not the product, so that removing/updating one line
     * among several for the same product is unambiguous.
     */
    public Optional<CartItem> findByItemId(String itemId) {
        return items.stream().filter(i -> i.getItemId().equals(itemId)).findFirst();
    }

    public boolean removeItemById(String itemId, Instant now) {
        boolean removed = items.removeIf(i -> i.getItemId().equals(itemId));
        if (removed) {
            this.updatedAt = now;
        }
        return removed;
    }

    /** Sets an existing line's quantity directly (looked up by itemId, not productId). */
    public void setItemQuantity(String itemId, int quantity, Instant now) {
        CartItem item = findByItemId(itemId)
                .orElseThrow(() -> new java.util.NoSuchElementException("no cart item with itemId " + itemId));
        item.setQuantity(quantity);
        this.updatedAt = now;
    }

    public void clear(Instant now) {
        items.clear();
        this.updatedAt = now;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public BigDecimal getSubtotal() {
        return items.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public String toString() {
        return "Cart[userId=" + userId + ", items=" + items.size() + ", subtotal=" + getSubtotal() + "]";
    }
}
