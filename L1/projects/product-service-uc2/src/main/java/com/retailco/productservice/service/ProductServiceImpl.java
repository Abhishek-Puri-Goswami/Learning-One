package com.retailco.productservice.service;

import com.retailco.productservice.dto.ProductPageResponse;
import com.retailco.productservice.dto.ProductRequest;
import com.retailco.productservice.dto.ProductResponse;
import com.retailco.productservice.exception.ProductNotFoundException;
import com.retailco.productservice.model.Product;
import com.retailco.productservice.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * This is the "Service" layer — where our actual business rules live, in
 * between the Controller and the Repository:
 * <pre>Controller → Service → Repository → storage</pre>
 * Keeping the rules here (like "give every new product a fresh id and
 * timestamp" or "throw a 404 if the product doesn't exist") means the
 * controller can stay simple and focused only on handling HTTP requests,
 * while all these rules live in one place that's easy to test on its own.
 */
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;

    public ProductServiceImpl(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public ProductPageResponse listProducts(int page, int size, String category) {
        List<Product> all = repository.findAll().stream()
                .filter(p -> category == null || category.isBlank() || p.getCategory().equalsIgnoreCase(category))
                .toList();
        return paginate(all, page, size);
    }

    @Override
    public ProductResponse getProductById(String id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return toResponse(product);
    }

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        Instant now = Instant.now();
        Product product = new Product(
                UUID.randomUUID().toString(),
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getCategory(),
                request.getStockQuantity(),
                now,
                now
        );
        repository.save(product);
        return toResponse(product);
    }

    @Override
    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product existing = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setPrice(request.getPrice());
        existing.setCategory(request.getCategory());
        existing.setStockQuantity(request.getStockQuantity());
        existing.setUpdatedAt(Instant.now());

        repository.save(existing);
        return toResponse(existing);
    }

    @Override
    public ProductPageResponse searchProducts(String query, int page, int size) {
        List<Product> matches = repository.search(query);
        return paginate(matches, page, size);
    }

    /**
     * Cuts the full list of matching products down to just one "page" of
     * results, and works out how many pages there are in total. This is a
     * simple, manual version of pagination — a real database-backed
     * version would use Spring Data's built-in {@code Pageable} feature
     * instead of loading everything into memory and slicing it here.
     */
    private ProductPageResponse paginate(List<Product> all, int page, int size) {
        int fromIndex = Math.min(page * size, all.size());
        int toIndex = Math.min(fromIndex + size, all.size());
        List<ProductResponse> content = all.subList(fromIndex, toIndex).stream()
                .map(this::toResponse)
                .toList();
        int totalPages = (int) Math.ceil((double) all.size() / size);
        return new ProductPageResponse(content, page, size, all.size(), totalPages);
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getCategory(), p.getStockQuantity(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
