package com.retailco.orderservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

// CONCEPT: Spring Boot entry point. Boots the app and scans for
// @Controller/@Service/@Repository classes.
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

    // CONCEPT: @Bean method with configurable timeouts -- without these,
    // an HTTP call (e.g. to a hung payment gateway) could block forever
    // instead of failing fast with a clear error.
    /**
     * L1/UC5 fix (see edge-cases/edge-case-catalog.md, "Payment timeout"):
     * the plain `new RestTemplate()` used in L1/UC2 and L1/UC4 has NO
     * connect/read timeout configured, meaning a hung payment gateway (or
     * cart/product service) call would block the calling thread
     * indefinitely instead of failing fast. Explicit timeouts turn "the
     * gateway never responds" into a distinguishable, testable
     * PaymentFailedException instead of an indefinite hang.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder,
                                      @Value("${http-client.connect-timeout-ms:2000}") long connectTimeoutMs,
                                      @Value("${http-client.read-timeout-ms:3000}") long readTimeoutMs) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }
}
