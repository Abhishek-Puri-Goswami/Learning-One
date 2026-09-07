package com.retailco.ecommerce.catalog;

import com.retailco.ecommerce.model.Product;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Think of this class as our in-memory "database table" of products —
 * it stands in for a real database here, but its job is the same: store
 * every product and be the single place where stock counts are checked
 * and changed.
 * <p>
 * Notice {@code reserveStock}, {@code releaseStock} and {@code restock}
 * are all marked {@code synchronized}. That keyword means "only one
 * thread can run this method at a time." We need it because two
 * customers could try to buy the last item at the exact same moment —
 * without this protection, both could see "1 left" and both succeed,
 * selling the same item twice. {@code synchronized} makes the
 * "check stock, then reduce it" sequence happen as one uninterruptible
 * step.
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
     * Takes stock away for one product — but only after confirming there's
     * enough left. Both the check and the reduction happen inside this one
     * {@code synchronized} method, so no other request can sneak in
     * between "checking" and "reducing" and cause us to oversell.
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
