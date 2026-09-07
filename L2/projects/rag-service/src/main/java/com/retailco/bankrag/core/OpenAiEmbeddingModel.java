package com.retailco.bankrag.core;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

// CONCEPT: AI/LLM integration -- a real embedding-API client, and the
// "Strategy" implementation that plugs into the EmbeddingModel interface.
// PURPOSE: This is the production embedding model. It calls OpenAI's
// /v1/embeddings endpoint over plain java.net.http.HttpClient (no SDK,
// no Jackson -- see MinimalJson) and converts the JSON response into the
// double[] vector the rest of the app (VectorStore) expects.
//
// FLOW (see embed() below, step by step):
// 1. Build a JSON request body {"model": ..., "input": text}.
// 2. POST it to {baseUrl}/embeddings with an Authorization: Bearer header.
// 3. If the HTTP status isn't 2xx, throw OpenAiApiException with the raw
//    error body attached -- callers see exactly what OpenAI said was wrong.
// 4. Otherwise parse response.data[0].embedding into a double[].
//
// WHY read the key/URL/model from environment variables (see the
// constructor and envOrDefault()) instead of hardcoding them: this keeps
// the secret out of source code entirely (never logged, never committed),
// and lets the exact same class talk to any OpenAI-compatible endpoint
// (OpenAI directly, Azure OpenAI, or a corporate gateway) just by changing
// OPENAI_BASE_URL -- no code change needed.
//
// IMPORTANT (fail-fast pattern): if OPENAI_API_KEY is missing/blank, the
// constructor throws IllegalStateException immediately rather than
// creating a half-working object. isConfigured() lets callers check
// BEFORE constructing, so they can choose LocalHashingEmbeddingModel
// instead -- see each project's *Config.java for that real/stub switch.
//
// WHAT IF REMOVED: without this class, the app could only ever produce
// hashed bag-of-words vectors (LocalHashingEmbeddingModel) -- it would
// still run, but retrieval quality would be limited to literal word
// overlap rather than true semantic similarity.
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
