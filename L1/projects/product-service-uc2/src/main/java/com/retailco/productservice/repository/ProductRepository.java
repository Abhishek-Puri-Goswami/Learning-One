package com.retailco.productservice.repository;

import com.retailco.ecommerce.catalog.ProductNotFoundException;
import com.retailco.productservice.model.Product;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

// CONCEPT: Repository pattern -- the layer that talks to storage, hiding
// HOW data is stored from the rest of the app.
// PURPOSE: Provides save/find/search operations for products. Internally,
// it just forwards to `ecommerce-core`'s ProductCatalog (a real,
// already-tested in-memory store), and converts between this module's
// `Product` DTO-like entity and ecommerce-core's own `Product` class.
// WHY delegate instead of reimplementing storage here: reusing an
// already-tested class means this repository doesn't need to re-prove
// basic rules like "never oversell" or "lookup by id" -- it only needs to
// get the conversion between the two `Product` shapes right.
// IMPORTANT: production would swap this out for a real database
// (PostgreSQL + Spring Data JPA) -- callers (ProductServiceImpl) would
// not need to change, since they only depend on this class's methods.
@Repository
public class ProductRepository {

    private final com.retailco.ecommerce.catalog.ProductCatalog catalog;

    public ProductRepository() {
        this.catalog = new com.retailco.ecommerce.catalog.ProductCatalog(Clock.systemUTC());
    }

    public Product save(Product product) {
        catalog.put(toCoreProduct(product));
        return product;
    }

    public Optional<Product> findById(String id) {
        try {
            return Optional.of(toDto(catalog.get(id)));
        } catch (ProductNotFoundException e) {
            return Optional.empty();
        }
    }

    public List<Product> findAll() {
        return catalog.findAll().stream().map(this::toDto).toList();
    }

    public List<Product> search(String query) {
        String lower = query.toLowerCase();
        return catalog.findAll().stream()
                .filter(p -> p.getName().toLowerCase().contains(lower)
                        || (p.getDescription() != null && p.getDescription().toLowerCase().contains(lower))
                        || p.getCategory().toLowerCase().contains(lower))
                .map(this::toDto)
                .toList();
    }

    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    private com.retailco.ecommerce.model.Product toCoreProduct(Product p) {
        return new com.retailco.ecommerce.model.Product(
                p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getCategory(), p.getStockQuantity(), p.getCreatedAt(), p.getUpdatedAt());
    }

    private Product toDto(com.retailco.ecommerce.model.Product core) {
        return new Product(core.getId(), core.getName(), core.getDescription(), core.getPrice(),
                core.getCategory(), core.getStockQuantity(), core.getCreatedAt(), core.getUpdatedAt());
    }
}
