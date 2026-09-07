package com.retailco.cartservice.client;

import com.retailco.cartservice.exception.InvalidProductException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

// CONCEPT: HTTP client wrapper, now distinguishing two different failure
// kinds instead of treating every failure the same way:
// - A genuine 404 (product truly doesn't exist) -> throws
//   InvalidProductException, which becomes a real error response.
// - Any other failure (timeout, connection refused, server error) ->
//   falls back to a placeholder, so the cart stays usable during an
//   outage.
// WHY separate them: an invalid product id is a data problem that should
// be rejected; a network hiccup is an availability problem that should
// degrade gracefully. Treating both the same way (as the earlier version
// of this class did) let bad product ids silently slip through at price 0.
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
            // Transient/infra failure (timeout, connection refused, 5xx) --
            // degrade gracefully so Cart stays usable even if Catalog is down.
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
