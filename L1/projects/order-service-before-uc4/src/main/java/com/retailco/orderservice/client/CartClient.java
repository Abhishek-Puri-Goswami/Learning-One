package com.retailco.orderservice.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class CartClient {

    private final RestTemplate restTemplate;

    public CartClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // FINDING (see reviews/): "hallucinated API" -- /checkout-summary was never
    // defined in L1/UC2's openapi/cart-service.yaml. cart-service only exposes
    // GET /api/v1/cart/{userId}. This endpoint returns 404 in production and
    // was never caught because there is no contract test tying this client to
    // the real OpenAPI spec.
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCheckoutSummary(String userId) {
        return restTemplate.getForObject(
                "http://localhost:8082/api/v1/cart/" + userId + "/checkout-summary",
                Map.class);
    }
}
