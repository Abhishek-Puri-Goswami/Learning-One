package com.retailco.bankrag.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The starting point of this application. Running this boots up Spring
 * Boot, which starts a web server and wires everything together.
 * <p>
 * The {@code scanBasePackages = "com.retailco.bankrag"} part matters
 * here: this class lives in the {@code integration} sub-package, but the
 * components it needs to find — the assistant, the core RAG pieces,
 * observability, and security — all live in sibling packages copied into
 * this same module. Without telling Spring to scan the whole
 * {@code com.retailco.bankrag} tree, it would only look inside
 * {@code integration} by default and miss all of those.
 */
@SpringBootApplication(scanBasePackages = "com.retailco.bankrag")
public class BankingSupportServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankingSupportServiceApplication.class, args);
    }
}
