package com.retailco.orderservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Talks to product-service over HTTP to check the CURRENT stock level for
 * a product, right before we charge the customer. Earlier, order-service
 * never did this — it simply trusted whatever snapshot cart-service gave
 * it, even though that snapshot could be old by the time checkout
 * happens. This class closes that gap by re-checking real stock at the
 * last possible moment, so we don't charge someone for an item that's
 * actually sold out.
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
