package com.retailco.bankrag.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * L2 UC2 deliverable: production-shaped Spring Boot entry point wrapping
 * rag-assistant-core's guardrail -> retrieval -> prompt -> generation ->
 * citation -> trace -> evaluation pipeline in a REST API.
 */
@SpringBootApplication
public class RagAssistantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RagAssistantServiceApplication.class, args);
    }
}
