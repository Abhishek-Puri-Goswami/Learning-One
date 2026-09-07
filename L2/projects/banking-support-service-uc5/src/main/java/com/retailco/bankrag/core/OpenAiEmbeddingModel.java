package com.retailco.bankrag.core;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * This is the REAL, production embedding model — it actually calls
 * OpenAI's embeddings API over the internet to turn text into meaningful
 * vectors, using nothing more than the JDK's own built-in HTTP client (no
 * external library needed).
 * <p>
 * Here's what happens when {@code embed()} is called, step by step:
 * <ol>
 *   <li>Build a small JSON request containing the model name and the
 *       text to embed.</li>
 *   <li>Send it to OpenAI with our API key in the request header.</li>
 *   <li>If OpenAI responds with an error, throw
 *       {@code OpenAiApiException} carrying OpenAI's own error message,
 *       so whoever's debugging sees exactly what went wrong.</li>
 *   <li>Otherwise, read the vector of numbers out of the response.</li>
 * </ol>
 * <p>
 * Notice the API key, base URL, and model name all come from environment
 * variables rather than being written directly in the code. That keeps
 * the secret key completely out of source control, and it also means the
 * exact same class can talk to a different OpenAI-compatible service
 * (Azure OpenAI, or a company's own gateway) just by changing an
 * environment variable — no code change needed.
 * <p>
 * If {@code OPENAI_API_KEY} isn't set, the constructor immediately throws
 * an error rather than quietly creating a broken object. That's why every
 * caller checks {@code isConfigured()} FIRST, before deciding whether to
 * use this class or fall back to {@code LocalHashingEmbeddingModel}
 * instead (see each module's Config class for that decision).
 */
public class OpenAiEmbeddingModel implements EmbeddingModel {

    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL = "text-embedding-3-small";
    private static final int DEFAULT_DIMENSIONS = 1536; // text-embedding-3-small's native size

    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int dimensions;

    public OpenAiEmbeddingModel() {
        this(System.getenv("OPENAI_API_KEY"),
                envOrDefault("OPENAI_BASE_URL", DEFAULT_BASE_URL),
                envOrDefault("OPENAI_EMBEDDING_MODEL", DEFAULT_MODEL),
                envIntOrDefault("OPENAI_EMBEDDING_DIMENSIONS", DEFAULT_DIMENSIONS));
    }

    OpenAiEmbeddingModel(String apiKey, String baseUrl, String model, int dimensions) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY environment variable is not set -- cannot construct "
                            + "OpenAiEmbeddingModel. Fall back to LocalHashingEmbeddingModel instead.");
        }
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.dimensions = dimensions;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /** True when a usable {@code OPENAI_API_KEY} is present in the environment. */
    public static boolean isConfigured() {
        String key = System.getenv("OPENAI_API_KEY");
        return key != null && !key.isBlank();
    }

    @Override
    public double[] embed(String text) {
        String requestBody = "{"
                + "\"model\":" + MinimalJson.quote(model) + ","
                + "\"input\":" + MinimalJson.quote(text)
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/embeddings"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new OpenAiApiException(
                        "OpenAI embeddings call failed with HTTP " + response.statusCode()
                                + ": " + response.body());
            }
            Map<String, Object> root = MinimalJson.asObject(MinimalJson.parse(response.body()));
            List<Object> data = MinimalJson.asArray(root.get("data"));
            Map<String, Object> first = MinimalJson.asObject(data.get(0));
            List<Object> embeddingJson = MinimalJson.asArray(first.get("embedding"));
            double[] vector = new double[embeddingJson.size()];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = MinimalJson.asDouble(embeddingJson.get(i));
            }
            return vector;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new OpenAiApiException("OpenAI embeddings call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    private static String envOrDefault(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private static int envIntOrDefault(String name, int fallback) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) return fallback;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
