package com.retailco.cartservice.client;

import com.retailco.cartservice.exception.InvalidProductException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * L1/UC5 fix (see edge-cases/edge-case-catalog.md, "Invalid product ID"):
 * a genuine 404 from product-service now throws InvalidProductException
 * (propagated by CartServiceImpl as a 404 to the caller) instead of being
 * silently treated the same as a transient network failure. A real
 * infra/network failure (timeout, connection refused, 5xx) still degrades
 * gracefully to the fallback snapshot, since that is a resilience concern,
 * not a data-validity concern -- conflating the two was the original bug.
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
