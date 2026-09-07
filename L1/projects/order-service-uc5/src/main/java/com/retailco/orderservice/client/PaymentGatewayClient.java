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
 * Carried forward from L1/UC4 (externalized API key, no more swallowed
 * exceptions). L1/UC5 addition: ResourceAccessException (what RestTemplate
 * throws for a connect/read timeout, per the timeout config added in
 * OrderServiceApplication) is now caught separately so a timed-out payment
 * call produces a distinguishable, testable message instead of being lumped
 * in with every other RestClientException. See
 * edge-cases/edge-case-catalog.md, "Payment timeout".
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
