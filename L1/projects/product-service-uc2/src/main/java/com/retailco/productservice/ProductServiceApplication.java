package com.retailco.productservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This is where the whole application starts. Running this class's
 * {@code main} method boots up the entire Spring Boot app: it starts a
 * built-in web server, scans this package for our
 * {@code @Controller}/{@code @Service}/{@code @Repository} classes, and
 * wires them all together automatically.
 */
@SpringBootApplication
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
