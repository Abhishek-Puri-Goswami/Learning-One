package com.retailco.bankrag.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// CONCEPT: Spring Boot application entry point.
// PURPOSE: `@SpringBootApplication` is a combination of three annotations
// (@Configuration, @EnableAutoConfiguration, @ComponentScan) that tells
// Spring Boot to: treat this class as a source of bean definitions, guess
// and auto-configure sensible defaults (embedded Tomcat, JSON support,
// etc.) based on what's on the classpath, and scan this package and its
// sub-packages for @Component/@Service/@Controller/@Configuration classes
// to register as Spring beans.
// FLOW: `SpringApplication.run(...)` boots the whole application context:
// it discovers RagCoreConfig's @Bean methods, wires them into
// IngestionService/SearchService, registers the @RestController classes,
// and starts an embedded web server -- all from this one method call.
// WHY this file is so small: that's the point of Spring Boot's
// auto-configuration -- almost everything else in this module (beans,
// routing, JSON handling) is declared declaratively elsewhere
// (@Configuration, @Bean, @RestController), not wired manually here.
@SpringBootApplication
public class RagServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RagServiceApplication.class, args);
    }
}
