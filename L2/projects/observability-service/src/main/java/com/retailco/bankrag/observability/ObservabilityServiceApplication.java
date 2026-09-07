package com.retailco.bankrag.observability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The starting point of this application. Running this boots up the
 * whole module: it discovers {@code ObservabilityConfig}'s beans
 * ({@code EmbeddingModel}, {@code LlmClient}, {@code RagAssistant},
 * {@code QueryCache}, {@code MetricsRecorder}, {@code CostEstimator},
 * {@code ObservableRagAssistant}) and the REST controllers, and starts the
 * built-in web server.
 */
@SpringBootApplication
public class ObservabilityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ObservabilityServiceApplication.class, args);
    }
}
