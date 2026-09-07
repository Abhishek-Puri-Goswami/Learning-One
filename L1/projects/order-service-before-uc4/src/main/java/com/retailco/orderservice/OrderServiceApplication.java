package com.retailco.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * The starting point of this application. Running this boots up Spring
 * Boot, which starts a web server and wires together our
 * {@code @Controller}, {@code @Service}, and {@code @Repository} classes.
 */
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

    /**
     * Registers a {@code RestTemplate} — a simple HTTP client — so it can
     * be automatically given to any class that needs one, like
     * {@code CartClient} or {@code PaymentGatewayClient}.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
