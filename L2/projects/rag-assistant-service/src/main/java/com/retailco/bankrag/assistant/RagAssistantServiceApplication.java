package com.retailco.bankrag.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The starting point of this application. Running this boots up the
 * whole assistant service: it discovers {@code AssistantConfig}'s beans
 * ({@code EmbeddingModel}, {@code LlmClient}, {@code RagAssistant}),
 * registers our REST controllers, and starts the built-in web server.
 */
@SpringBootApplication
public class RagAssistantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RagAssistantServiceApplication.class, args);
    }
}
