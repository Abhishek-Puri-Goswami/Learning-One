package com.retailco.cartservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * The starting point of this application. Running this class boots up
 * Spring Boot, which starts a web server and automatically finds and
 * connects all our {@code @Controller}, {@code @Service}, and
 * {@code @Repository} classes.
 */
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
