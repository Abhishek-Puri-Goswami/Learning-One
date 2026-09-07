package com.retailco.bankrag.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The starting point of this application. {@code @SpringBootApplication}
 * combines three annotations that together tell Spring Boot to: treat
 * this class as a source of bean definitions, automatically configure
 * sensible defaults (an embedded web server, JSON support, etc.) based on
 * what's on the classpath, and scan this package (and everything under
 * it) for our {@code @Component}/{@code @Service}/{@code @Controller}/
 * {@code @Configuration} classes to register.
 * <p>
 * Calling {@code SpringApplication.run(...)} boots the ENTIRE
 * application: it discovers {@code RagCoreConfig}'s beans, wires them
 * into our services, registers the REST controllers, and starts the web
 * server — all from this one method call.
 * <p>
 * Notice how small this file is. That's exactly the point of Spring
 * Boot's auto-configuration: almost everything else (beans, routing,
 * JSON handling) is declared elsewhere, using annotations, rather than
 * wired together manually here.
 */
@SpringBootApplication
public class RagServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RagServiceApplication.class, args);
    }
}
