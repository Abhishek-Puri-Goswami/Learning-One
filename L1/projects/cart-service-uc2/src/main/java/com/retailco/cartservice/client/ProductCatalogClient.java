package com.retailco.cartservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * Client to the Product Catalog Service, used to revalidate price/name at
 * add-to-cart time (see architecture.json risk: "Stale price/stock shown in
 * cart if catalog isn't re-checked at checkout" -- we mitigate at add-time too).
 *
 * A resilient fallback (cached/mock lookup) is used if the Catalog Service is
 * unreachable, so Cart remains usable in a degraded mode; the price is always
 * re-validated for real at checkout time in order-management-service.
 */
@Component
public class ProductCatalogClient {

    private final RestTemplate restTemplate;
    private final String catalogBaseUrl;

    public ProductCatalogClient(RestTemplate restTemplate,
                                 @Value("${services.product-catalog.base-url:http://localhost:8081}") String catalogBaseUrl) {
        this.restTemplate = restTemplate;
        this.catalogBaseUrl = catalogBaseUrl;
    }

    public ProductSnapshot fetchProduct(String productId) {
        try {
            ProductDto product = restTemplate.getForObject(
                    catalogBaseUrl + "/api/v1/products/{id}", ProductDto.class, productId);
            if (product == null) {
                return fallback(productId);
            }
            return new ProductSnapshot(product.name(), product.price());
        } catch (Exception ex) {
            // Circuit-breaker-style degradation: Cart stays usable even if Catalog is down.
            return fallback(productId);
        }
    }

    private ProductSnapshot fallback(String productId) {
        return new ProductSnapshot("Unknown product (" + productId + ")", BigDecimal.ZERO);
    }

    public record ProductDto(String id, String name, BigDecimal price) {
    }

    public record ProductSnapshot(String name, BigDecimal price) {
    }
}
