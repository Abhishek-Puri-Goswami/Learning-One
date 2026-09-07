package com.retailco.orderservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

/**
 * This class talks to cart-service over HTTP to fetch a customer's cart.
 * This is the fixed version of the file — compare it with
 * {@code order-service-before-uc4}'s version to see the difference: that
 * one called a URL, {@code /checkout-summary}, that cart-service never
 * actually exposed. This version calls the REAL endpoint,
 * {@code GET /api/v1/cart/{userId}}, and reads the response using the
 * exact shape cart-service actually returns (a list of items plus a
 * subtotal) — the lesson being: always build against the real, documented
 * API of a service, not an assumption about what it might look like.
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
