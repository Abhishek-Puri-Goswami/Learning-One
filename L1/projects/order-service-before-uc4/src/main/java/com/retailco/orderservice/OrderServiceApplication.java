package com.retailco.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

// CONCEPT: Spring Boot entry point. Boots the app and scans for
// @Controller/@Service/@Repository classes.
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

    // Registers a RestTemplate bean so it can be injected into HTTP client
    // classes like CartClient/PaymentGatewayClient.
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
