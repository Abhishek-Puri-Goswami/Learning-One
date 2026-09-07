package com.retailco.orderservice.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

/**
 * This class is meant to talk to an external payment gateway over HTTP.
 * This is the "before" version of the file, kept as a learning example —
 * it has two real problems worth understanding, both fixed in
 * {@code order-service-refactored-uc4}'s version of this same file:
 * <ol>
 *   <li>The API key below is hard-coded directly into the source code.
 *       That's risky: anyone who can read the code (or the git history)
 *       can see the key, and changing it requires a brand-new deployment
 *       instead of just updating a configuration value.</li>
 *   <li>Look at {@code charge()} — it always returns {@code true}, even
 *       when the actual HTTP call fails! That means a real payment
 *       failure would be silently reported as a success.</li>
 * </ol>
 */
@Component
public class PaymentGatewayClient {

    private static final String PAYMENT_API_KEY = "<API_KEY>";

    private final RestTemplate restTemplate;

    public PaymentGatewayClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean charge(BigDecimal amount, String paymentMethod) {
        try {
            Map<String, Object> body = Map.of(
                    "amount", amount,
                    "method", paymentMethod,
                    "apiKey", PAYMENT_API_KEY);
            restTemplate.postForObject("https://payments.example.com/v1/charge", body, Map.class);
            return true;
        } catch (Exception e) {
            // This catch block does nothing with the exception — it just
            // swallows it silently. That's a real problem: if the payment
            // call actually failed, whoever called charge() has no way to
            // find out, because the method returns true no matter what,
            // right below.
        }
        return true;
    }
}
