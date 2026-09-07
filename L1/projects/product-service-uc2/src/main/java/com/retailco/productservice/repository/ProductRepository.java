package com.retailco.productservice.repository;

import com.retailco.ecommerce.catalog.ProductNotFoundException;
import com.retailco.productservice.model.Product;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

/**
 * A "Repository" is the layer that knows how to save and load data,
 * hiding the storage details from the rest of the app. This one doesn't
 * reinvent storage itself — it forwards everything to
 * {@code ecommerce-core}'s already-built and already-tested
 * {@code ProductCatalog}, and just converts between that module's
 * {@code Product} class and this service's own {@code Product} class.
 * <p>
 * Reusing that tested class means we don't have to re-prove basic rules
 * like "never sell more stock than we have" — we only need to get the
 * conversion between the two shapes right.
 * <p>
 * Later, this class could be swapped out for one backed by a real
 * database (PostgreSQL, for example) without {@code ProductServiceImpl}
 * having to change at all, since it only ever talks to this class's
 * methods.
 */
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
