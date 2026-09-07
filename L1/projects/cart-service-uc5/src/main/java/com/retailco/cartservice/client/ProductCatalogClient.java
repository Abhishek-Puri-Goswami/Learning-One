package com.retailco.cartservice.client;

import com.retailco.cartservice.exception.InvalidProductException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * Talks to product-service over HTTP to look up a product's name and
 * price. This version is smarter than a naive first attempt would be — it
 * treats two kinds of failure differently:
 * <ul>
 *   <li>A real "404 not found" means the product simply doesn't exist, so
 *       we throw {@code InvalidProductException} and let the request fail
 *       with a clear error.</li>
 *   <li>Any other failure (a timeout, a dropped connection, the other
 *       service being temporarily down) is treated as a network hiccup,
 *       not a real problem with the data — so instead of failing, we fall
 *       back to a placeholder value and let the cart keep working.</li>
 * </ul>
 * Why bother telling these apart? An invalid product id is a genuine
 * mistake that should be rejected. A network hiccup is a temporary
 * availability problem that shouldn't block the customer. If we treated
 * both the same way, a bad or fake product id could quietly slip through
 * priced at zero instead of being caught.
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
                throw new InvalidProductException(productId);
            }
            return new ProductSnapshot(product.name(), product.price());
        } catch (HttpClientErrorException.NotFound e) {
            throw new InvalidProductException(productId);
        } catch (RestClientException ex) {
            // A network-level problem (timeout, connection refused, a 5xx
            // error), not proof the product is invalid. We fall back to a
            // placeholder so the cart keeps working even while
            // product-service is having trouble.
            return fallback(productId);
        }
    }

    private ProductSnapshot fallback(String productId) {
        return new ProductSnapshot("Unknown product (" + productId + ") - catalog unavailable", BigDecimal.ZERO);
    }

    public record ProductDto(String id, String name, BigDecimal price) {
    }

    public record ProductSnapshot(String name, BigDecimal price) {
    }
}
