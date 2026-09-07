package com.retailco.cartservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

// CONCEPT: HTTP client wrapper -- calls another microservice
// (product-service) over REST, instead of a local method call.
// PURPOSE: Fetches a product's current name/price when adding it to a
// cart, so the cart shows up-to-date info.
// WHY the try/catch fallback: if product-service is down or slow, this
// still returns something (a placeholder "Unknown product") instead of
// crashing the whole add-to-cart request. This is a simple form of
// graceful degradation -- the cart stays usable even if a dependency fails.
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
