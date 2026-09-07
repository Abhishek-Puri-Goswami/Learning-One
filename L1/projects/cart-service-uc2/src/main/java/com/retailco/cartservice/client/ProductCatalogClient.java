package com.retailco.cartservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * This class talks to a completely different microservice — product-service
 * — over the network (HTTP), instead of just calling a local Java method.
 * Its job is simple: look up a product's current name and price so the
 * cart can show accurate info when an item is added.
 * <p>
 * Notice the try/catch below: if product-service is slow or completely
 * down, instead of crashing the whole "add to cart" request, we fall back
 * to a placeholder value ("Unknown product"). This is a simple example of
 * "graceful degradation" — the cart keeps working even when one of its
 * dependencies has a problem.
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
