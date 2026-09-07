package com.retailco.orderservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

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
     * Registers our HTTP client with sensible timeouts. Earlier versions
     * of this project used a plain {@code new RestTemplate()} with no
     * timeout at all — which means if a service we call (like the payment
     * gateway) ever hangs and never responds, our own request would wait
     * forever too, freezing up the thread handling it. Setting explicit
     * connect and read timeouts here means a slow or unresponsive service
     * fails fast with a clear error, instead of hanging indefinitely.
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
