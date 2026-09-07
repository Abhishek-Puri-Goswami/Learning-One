package com.retailco.bankrag.assistant;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

// CONCEPT: AI/LLM integration -- a real Chat Completions API client, and
// the Strategy implementation that plugs into the LlmClient interface.
// PURPOSE: This is the production text-generation model. It sends the
// fully-built prompt (from PromptTemplate) to OpenAI's
// /v1/chat/completions endpoint as a single user message, and turns the
// JSON response into the LlmResponse (text + token counts) the rest of
// the app expects.
//
// FLOW (see generate() below, step by step):
// 1. Build a JSON chat-completion request: {"model", "messages": [{role:
//    "user", content: prompt}], "temperature": 0.2}. Low temperature keeps
//    answers more deterministic/factual, appropriate for a policy Q&A
//    assistant that must not "creatively" invent banking rules.
// 2. POST it to {baseUrl}/chat/completions with a Bearer auth header.
// 3. Non-2xx status -> throw OpenAiApiException with OpenAI's raw error
//    body attached.
// 4. Parse response.choices[0].message.content as the answer text, and
//    response.usage.prompt_tokens/completion_tokens for real token counts
//    (used later for cost estimation -- see observability/CostEstimator).
//
// WHY the prompt is sent completely unchanged as one message: this keeps
// this class a drop-in replacement for ExtractiveStubLlmClient -- both
// implement the exact same LlmClient.generate(String prompt) contract, so
// RagAssistant's orchestration code never needs to know or care which one
// is actually running.
//
// SAME env-var/fail-fast pattern as OpenAiEmbeddingModel (see that class's
// comments in core/OpenAiEmbeddingModel.java for the full reasoning):
// OPENAI_API_KEY/OPENAI_BASE_URL/OPENAI_CHAT_MODEL come from the
// environment, never hardcoded; isConfigured() lets callers check before
// constructing so they can fall back to ExtractiveStubLlmClient instead.
public class OpenAiLlmClient implements LlmClient {

    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public OpenAiLlmClient() {
        this(System.getenv("OPENAI_API_KEY"),
                envOrDefault("OPENAI_BASE_URL", DEFAULT_BASE_URL),
                envOrDefault("OPENAI_CHAT_MODEL", DEFAULT_MODEL));
    }

    OpenAiLlmClient(String apiKey, String baseUrl, String model) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY environment variable is not set -- cannot construct "
                            + "OpenAiLlmClient. Fall back to ExtractiveStubLlmClient instead.");
        }
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
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
    public LlmResponse generate(String prompt) {
        String requestBody = "{"
                + "\"model\":" + MinimalJson.quote(model) + ","
                + "\"messages\":[{\"role\":\"user\",\"content\":" + MinimalJson.quote(prompt) + "}],"
                + "\"temperature\":0.2"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new OpenAiApiException(
                        "OpenAI chat completions call failed with HTTP " + response.statusCode()
                                + ": " + response.body());
            }
            Map<String, Object> root = MinimalJson.asObject(MinimalJson.parse(response.body()));
            List<Object> choices = MinimalJson.asArray(root.get("choices"));
            Map<String, Object> firstChoice = MinimalJson.asObject(choices.get(0));
            Map<String, Object> message = MinimalJson.asObject(firstChoice.get("message"));
            String content = (String) message.get("content");

            int promptTokens = 0;
            int completionTokens = 0;
            Object usageObj = root.get("usage");
            if (usageObj != null) {
                Map<String, Object> usage = MinimalJson.asObject(usageObj);
                if (usage.get("prompt_tokens") != null) promptTokens = MinimalJson.asInt(usage.get("prompt_tokens"));
                if (usage.get("completion_tokens") != null) {
                    completionTokens = MinimalJson.asInt(usage.get("completion_tokens"));
                }
            }

            return new LlmResponse(content, promptTokens, completionTokens);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new OpenAiApiException("OpenAI chat completions call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String modelId() {
        return model + " (real OpenAI call)";
    }

    private static String envOrDefault(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
