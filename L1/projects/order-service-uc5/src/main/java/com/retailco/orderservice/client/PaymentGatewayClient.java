package com.retailco.orderservice.client;

import com.retailco.orderservice.exception.PaymentFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Talks to an external payment gateway over HTTP. This version adds one
 * more improvement on top of the earlier fixes (config-driven API key, no
 * swallowed exceptions): it now catches {@code ResourceAccessException}
 * separately from other failures. That's the specific exception Spring
 * throws when a call times out (see {@code OrderServiceApplication}'s
 * timeout configuration) — catching it separately lets us say clearly
 * "the payment gateway timed out" instead of a vague "something went
 * wrong," which is much easier to diagnose when it happens.
 */
@Component
public class PaymentGatewayClient {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String gatewayBaseUrl;

    public PaymentGatewayClient(RestTemplate restTemplate,
                                 @Value("${payment.gateway.api-key}") String apiKey,
                                 @Value("${payment.gateway.base-url}") String gatewayBaseUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.gatewayBaseUrl = gatewayBaseUrl;
    }

    public void charge(BigDecimal amount, String paymentMethod) {
        try {
            Map<String, Object> body = Map.of(
                    "amount", amount,
                    "method", paymentMethod,
                    "apiKey", apiKey
            );
            restTemplate.postForObject(gatewayBaseUrl + "/v1/charge", body, Map.class);
        } catch (ResourceAccessException e) {
            throw new PaymentFailedException(
                    "Payment gateway timed out for amount " + amount + " via " + paymentMethod, e);
        } catch (RestClientException e) {
            throw new PaymentFailedException(
                    "Payment gateway call failed for amount " + amount + " via " + paymentMethod, e);
        }
    }
}
