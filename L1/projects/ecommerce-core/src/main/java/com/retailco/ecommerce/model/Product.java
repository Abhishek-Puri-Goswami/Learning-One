package com.retailco.ecommerce.model;

import java.math.BigDecimal;
import java.time.Instant;

// CONCEPT: Domain model with self-validation in the constructor.
// PURPOSE: Represents one product (id, name, price, stock, etc.) and makes
// sure it can never exist in an invalid state -- the constructor rejects
// a blank id, negative price, or negative stock right away.
// WHY stock can only change via adjustStock(): keeping ALL stock changes
// going through one method (rather than a public setter) is what lets
// ProductCatalog guarantee stock never goes negative -- one choke point,
// one place to enforce the rule.
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

    // The only way stock quantity ever changes. `delta` is negative when
    // checkout reserves stock, positive when stock is restocked/released.
    // Throws instead of silently clamping to 0, so the caller
    // (ProductCatalog) can turn this into a proper error rather than let
    // stock quietly go wrong.
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
