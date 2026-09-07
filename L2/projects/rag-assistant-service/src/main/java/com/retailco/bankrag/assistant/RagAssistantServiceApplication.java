package com.retailco.bankrag.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// CONCEPT: Spring Boot application entry point (see rag-service's
// RagServiceApplication for the full explanation of @SpringBootApplication
// and SpringApplication.run() -- identical mechanism here).
// PURPOSE: Boots the whole assistant service: discovers AssistantConfig's
// beans (EmbeddingModel, LlmClient, RagAssistant), registers the
// @RestController classes (AskController, IngestionController), and
// starts the embedded web server.
@SpringBootApplication
public class RagAssistantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RagAssistantServiceApplication.class, args);
    }
}
