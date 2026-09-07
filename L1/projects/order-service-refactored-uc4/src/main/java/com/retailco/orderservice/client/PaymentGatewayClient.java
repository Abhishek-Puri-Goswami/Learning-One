package com.retailco.orderservice.client;

import com.retailco.orderservice.exception.PaymentFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

/**
 * This class talks to an external payment gateway over HTTP. This is the
 * fixed version of the file, and it fixes two real problems from the
 * "before" version:
 * <ol>
 *   <li>The API key is no longer typed directly into the source code.
 *       Instead, it's read from configuration using {@code @Value} — in a
 *       real deployment, that value would come from an environment
 *       variable or a secrets manager, never be committed to git.</li>
 *   <li>If the call to the gateway fails, this class now throws a
 *       {@link PaymentFailedException} instead of quietly hiding the
 *       error. That means whoever calls {@code charge()} — namely
 *       {@code OrderServiceImpl} — is forced to deal with the failure,
 *       so a failed charge can never be mistaken for a successful one.</li>
 * </ol>
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
