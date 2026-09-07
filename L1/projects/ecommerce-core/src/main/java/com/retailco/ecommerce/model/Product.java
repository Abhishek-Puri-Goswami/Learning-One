package com.retailco.ecommerce.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Mirrors the field names/types already present in this submission's
 * L1/UC2 {@code product-service}'s existing (written-but-never-compiled)
 * {@code Product} POJO: id, name, description, price, category,
 * stockQuantity, createdAt, updatedAt. This core module keeps the same
 * shape so the L1/UC2 Spring Boot wrapper can sit on top of it without a
 * field-by-field remap.
 *
 * <p>Mutable on purpose (mirrors a JPA-style entity) but stock mutation is
 * only ever done through {@link com.retailco.ecommerce.catalog.ProductCatalog#reserveStock}
 * so a single choke point can enforce "never go negative."
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
     * The single choke point for stock mutation. {@code delta} is negative for a
     * reservation (checkout consuming stock) and positive for a restock. Throws
     * rather than silently clamping so {@link com.retailco.ecommerce.catalog.ProductCatalog}
     * can translate this into an {@code InsufficientStockException} instead of
     * ever persisting a negative quantity.
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
