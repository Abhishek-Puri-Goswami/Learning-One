package com.retailco.ecommerce.catalog;

import com.retailco.ecommerce.model.Product;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory system of record for products and stock, standing in for
 * L1/UC2's {@code product-service} JPA repository (this submission has no
 * database, and Maven Central -- so no Spring Data JPA -- is blocked; see
 * this module's README). {@link #reserveStock} is the single choke point
 * every stock decrement goes through, so "never oversell" is enforced here
 * once rather than re-implemented at every caller.
 */
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

    /**
     * Atomically decrements stock for a single product. Synchronized per-catalog
     * (not just per-product) -- this is a small illustrative in-memory store, not
     * a throughput-critical one, so a coarse lock keeping the "check then decrement"
     * sequence atomic is preferable to a subtler per-row lock that's easy to get wrong.
     *
     * @throws ProductNotFoundException if productId is unknown
     * @throws InsufficientStockException if requestedQuantity exceeds current stock
     */
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
