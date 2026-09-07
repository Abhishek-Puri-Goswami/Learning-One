package com.retailco.orderservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * L1/UC5 addition: order-service did not previously call product-service at
 * all -- it trusted cart-service's snapshot of price/name unconditionally
 * and never re-checked current stock. This client re-validates stock
 * immediately before payment (see edge-cases/edge-case-catalog.md,
 * "Out-of-stock"), consistent with L1/UC1's stated design: "Cart is
 * intentionally NOT the system of record for price/stock."
 */
@Component
public class ProductClient {

    private final RestTemplate restTemplate;
    private final String productServiceBaseUrl;

    public ProductClient(RestTemplate restTemplate,
                          @Value("${services.product-catalog.base-url}") String productServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.productServiceBaseUrl = productServiceBaseUrl;
    }

    public int getAvailableStock(String productId) {
        ProductDto product = restTemplate.getForObject(
                productServiceBaseUrl + "/api/v1/products/{id}", ProductDto.class, productId);
        return product == null ? 0 : product.stockQuantity();
    }

    // Mirrors openapi/product-service.yaml components.schemas.ProductResponse (L1/UC2).
    public record ProductDto(String id, String name, String description, Object price,
                              String category, int stockQuantity) {
    }
}
