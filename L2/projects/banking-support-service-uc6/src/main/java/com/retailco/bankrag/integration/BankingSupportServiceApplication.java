package com.retailco.bankrag.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// CONCEPT: Spring Boot application entry point (see rag-service's
// RagServiceApplication for the full @SpringBootApplication explanation).
// IMPORTANT: `scanBasePackages = "com.retailco.bankrag"` is needed here
// specifically because this class lives in the `integration` sub-package,
// but the components it needs to discover span SEVERAL sibling packages
// (assistant, core, observability, security, integration) that are all
// copied-as-source into this one module. Without this explicit override,
// Spring's default component scan (which only scans the package of the
// @SpringBootApplication class and its sub-packages) would miss
// @Configuration/@Component classes living in those sibling packages.
@SpringBootApplication(scanBasePackages = "com.retailco.bankrag")
public class BankingSupportServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankingSupportServiceApplication.class, args);
    }
}
