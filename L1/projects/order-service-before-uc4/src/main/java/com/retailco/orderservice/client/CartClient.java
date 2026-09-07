package com.retailco.orderservice.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * This class is meant to talk to cart-service over HTTP to get a
 * customer's cart. This is the "before" version of the file, kept around
 * on purpose as a learning example — compare it with the same file in
 * {@code order-service-refactored-uc4} to see what changed and why.
 * <p>
 * Here's the bug: the URL below calls a
 * {@code /checkout-summary} endpoint that cart-service doesn't actually
 * have — cart-service only ever exposes {@code GET /api/v1/cart/{userId}}.
 * Calling a URL that doesn't exist means this method will fail with a 404
 * every time it runs against the real service. It slipped through because
 * nothing here was ever tested against cart-service's real, documented
 * API — a good reminder to always double-check the exact endpoints and
 * shapes another service actually promises to support.
 */
@Component
public class CartClient {

    private final RestTemplate restTemplate;

    public CartClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getCheckoutSummary(String userId) {
        return restTemplate.getForObject(
                "http://localhost:8082/api/v1/cart/" + userId + "/checkout-summary",
                Map.class);
    }
}
