package com.retailco.productservice.model;

import java.math.BigDecimal;
import java.time.Instant;

// CONCEPT: Domain model (entity) -- a plain Java class representing one
// product in the catalog.
// PURPOSE: Holds a product's data (name, price, stock, etc.) as it's kept
// in storage. Every field has a getter/setter pair (standard JavaBean
// style) so frameworks and other code can read/update it field by field.
// WHY: In-memory for now, not yet a real database entity. A real system
// would eventually store this in PostgreSQL via Spring Data JPA.
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
