package com.retailco.orderservice.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class PaymentGatewayClient {

    // FINDING (see reviews/): hard-coded credential (Sonar rule squid:S2068,
    // "Credentials should not be hard-coded"). This key is committed to
    // source control in plaintext and cannot be rotated without a code
    // deploy; it is also visible to anyone with repo read access, violating
    // ADR-003's requirement (L1/UC1) that payment integration be reviewed
    // under stricter security controls.    
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
            // FINDING (see reviews/): empty/near-empty catch block swallows the
            // exception (Sonar rule squid:S1166, "Exception handlers should
            // preserve the original exceptions"). Caller has no way to know a
            // payment actually failed -- charge() returns true below regardless.
        }
        return true;
    }
}
