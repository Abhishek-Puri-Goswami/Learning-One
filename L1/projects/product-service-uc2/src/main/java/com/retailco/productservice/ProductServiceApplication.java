package com.retailco.productservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// CONCEPT: Spring Boot entry point. Starting this class boots the whole
// app: it auto-configures a web server, scans this package for
// @Controller/@Service/@Repository classes, and wires them together.
@SpringBootApplication
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
