package com.retailco.ecommerce.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * This class represents one product in our store — think of it as the
 * blueprint for "a single item you could buy," holding its name, price,
 * category and how many are left in stock.
 * <p>
 * Notice the constructor checks its inputs before creating the object: no
 * blank id, no negative price, no negative stock. This is called
 * "self-validation" — the object protects itself from ever being created
 * in a broken state, so nothing else in the app has to double-check it later.
 * <p>
 * Stock quantity is special: it can only be changed through
 * {@link #adjustStock}, never through a plain setter. Funnelling every
 * stock change through one method is what lets {@code ProductCatalog}
 * guarantee stock can never go below zero — there is exactly one place
 * where that rule is enforced, instead of many places that could each
 * forget to check it.
 */
public class Product {

    private final String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private int stockQuantity;
    private final Instant createdAt;
    private Instant updatedAt;

    public Product(String id, String name, String description, BigDecimal price,
                   String category, int stockQuantity, Instant createdAt, Instant updatedAt) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("price must be non-negative: " + price);
        }
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("stockQuantity must be non-negative: " + stockQuantity);
        }
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.stockQuantity = stockQuantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getStockQuantity() { return stockQuantity; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * The one and only way this product's stock count changes.
     * Pass a negative {@code delta} to take stock away (e.g. a customer is
     * buying some) or a positive one to add stock back (e.g. a return, or
     * a cancelled order releasing it). If the result would go below zero,
     * this method refuses and throws an error instead of quietly clamping
     * to zero — that way, whoever called it finds out immediately that
     * something doesn't add up, instead of the stock count silently
     * becoming wrong.
     */
    public void adjustStock(int delta, Instant now) {
        int next = this.stockQuantity + delta;
        if (next < 0) {
            throw new IllegalStateException(
                    "stock would go negative for product " + id + ": " + this.stockQuantity + " + " + delta);
        }
        this.stockQuantity = next;
        this.updatedAt = now;
    }

    @Override
    public String toString() {
        return "Product[id=" + id + ", name=" + name + ", price=" + price
                + ", category=" + category + ", stockQuantity=" + stockQuantity + "]";
    }
}
