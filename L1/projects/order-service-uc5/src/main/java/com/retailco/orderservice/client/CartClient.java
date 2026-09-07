package com.retailco.orderservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

// CONCEPT: HTTP client wrapper -- calls cart-service over REST. This is
// the FIXED version of this refactoring case study.
/**
 * FIX (was AI-API-1 / hallucinated endpoint): now calls the REAL, contract-
 * defined GET /api/v1/cart/{userId} from L1/UC2's openapi/cart-service.yaml,
 * and maps the documented CartResponse shape (items[], subtotal, ...) rather
 * than an invented "/checkout-summary" response.
 */
@Component
public class CartClient {

    private final RestTemplate restTemplate;
    private final String cartServiceBaseUrl;

    public CartClient(RestTemplate restTemplate,
                       @Value("${services.cart.base-url}") String cartServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.cartServiceBaseUrl = cartServiceBaseUrl;
    }

    public CartSnapshot getCart(String userId) {
        CartResponseDto response = restTemplate.getForObject(
                cartServiceBaseUrl + "/api/v1/cart/{userId}", CartResponseDto.class, userId);
        if (response == null || response.items() == null) {
            return new CartSnapshot(List.of());
        }
        return new CartSnapshot(response.items());
    }

    // Mirrors openapi/cart-service.yaml components.schemas.CartResponse / CartItemResponse exactly.
    public record CartResponseDto(String userId, List<CartItemDto> items, int itemCount,
                                   BigDecimal subtotal, String updatedAt) {
    }

    public record CartItemDto(String itemId, String productId, String productName,
                               BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
    }

    public record CartSnapshot(List<CartItemDto> items) {
    }
}
