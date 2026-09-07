package com.retailco.cartservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

// CONCEPT: Spring Boot entry point. Boots the app and scans for
// @Controller/@Service/@Repository classes to wire together.
@SpringBootApplication
public class CartServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CartServiceApplication.class, args);
    }

    // CONCEPT: @Bean method -- registers a RestTemplate (a simple HTTP
    // client) so it can be injected wherever needed (see ProductCatalogClient).
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
