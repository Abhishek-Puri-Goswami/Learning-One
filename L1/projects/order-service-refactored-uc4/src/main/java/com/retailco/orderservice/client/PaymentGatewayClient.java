package com.retailco.orderservice.client;

import com.retailco.orderservice.exception.PaymentFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

// CONCEPT: HTTP client wrapper -- calls an external payment gateway. This
// is the FIXED version: config-driven API key, and failures now throw
// instead of being silently swallowed.
/**
 * FIX (was AI-SEC-1 / squid:S2068): the API key is no longer a source-code
 * literal. It is injected from configuration (@Value), which in turn should
 * be sourced from an environment variable or a secrets manager (Vault, AWS
 * Secrets Manager, etc.) in every real environment -- never committed to
 * git. See application.yml / application-example.env in this module.
 *
 * FIX (was AI-SEC-2, squid:S1166, squid:S3516): failures are no longer
 * swallowed. Any exception from the gateway call is wrapped in a
 * PaymentFailedException and propagated to the caller, so a failed charge
 * can never be recorded as a successful order (see ADR-003, L1/UC1: payment
 * decision logic must be deterministic and auditable, not "best effort").
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
        } catch (RestClientException e) {
            throw new PaymentFailedException(
                    "Payment gateway call failed for amount " + amount + " via " + paymentMethod, e);
        }
    }
}
