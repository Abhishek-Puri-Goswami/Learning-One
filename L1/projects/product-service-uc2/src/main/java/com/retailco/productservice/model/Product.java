package com.retailco.productservice.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain entity for a catalog product.
 * NOTE: In-memory for this scaffold (L1 UC2). Production wiring to PostgreSQL
 * (Spring Data JPA) is defined per ADR-002 in L1/UC1 and left as an integration
 * step outside the scope of this contract-first scaffolding exercise.
 */
public class Product {

    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private int stockQuantity;
    private Instant createdAt;
    private Instant updatedAt;

    public Product() {
    }

    public Product(String id, String name, String description, BigDecimal price,
                    String category, int stockQuantity, Instant createdAt, Instant updatedAt) {
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
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
