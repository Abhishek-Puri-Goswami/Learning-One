package com.retailco.bankrag.observability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// CONCEPT: Spring Boot application entry point (see rag-service's
// RagServiceApplication for the full explanation). Boots this module's
// object graph: ObservabilityConfig's beans (EmbeddingModel, LlmClient,
// RagAssistant, QueryCache, MetricsRecorder, CostEstimator,
// ObservableRagAssistant) and the @RestController classes.
@SpringBootApplication
public class ObservabilityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ObservabilityServiceApplication.class, args);
    }
}
