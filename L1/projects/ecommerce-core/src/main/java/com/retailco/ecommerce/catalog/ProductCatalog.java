package com.retailco.ecommerce.catalog;

import com.retailco.ecommerce.model.Product;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// CONCEPT: In-memory repository -- stands in for a real database table.
// PURPOSE: Stores products keyed by id and is the ONE place stock
// quantities get checked and changed, via reserveStock()/releaseStock().
// WHY `synchronized` on reserveStock/releaseStock/restock: several
// requests could try to buy the same product at the same time. Without
// synchronization, two threads could both "check stock is enough" before
// either one decrements it, causing an oversell. `synchronized` makes the
// whole check-then-decrement sequence atomic (one thread at a time).
public class ProductCatalog {

    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final Clock clock;

    public ProductCatalog(Clock clock) {
        this.clock = clock;
    }

    public void put(Product product) {
        products.put(product.getId(), product);
    }

    public Product get(String productId) {
        Product p = products.get(productId);
        if (p == null) {
            throw new ProductNotFoundException(productId);
        }
        return p;
    }

    public Collection<Product> findByCategory(String category) {
        return products.values().stream()
                .filter(p -> p.getCategory() != null && p.getCategory().equalsIgnoreCase(category))
                .toList();
    }

    public List<Product> findAll() {
        return List.copyOf(products.values());
    }

    // Reduces stock for one product -- checks there's enough stock FIRST,
    // then decrements, all inside one `synchronized` call so no other
    // thread can interleave and cause an oversell.
    public synchronized void reserveStock(String productId, int requestedQuantity) {
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("requestedQuantity must be positive: " + requestedQuantity);
        }
        Product product = get(productId);
        if (product.getStockQuantity() < requestedQuantity) {
            throw new InsufficientStockException(productId, requestedQuantity, product.getStockQuantity());
        }
        product.adjustStock(-requestedQuantity, Instant.now(clock));
    }

    /** Reverses a reservation -- used when a payment fails after stock was already reserved. */
    public synchronized void releaseStock(String productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        Product product = get(productId);
        product.adjustStock(quantity, Instant.now(clock));
    }

    public synchronized void restock(String productId, int quantity) {
        releaseStock(productId, quantity);
    }
}
