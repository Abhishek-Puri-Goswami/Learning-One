package com.retailco.bankrag.assistant;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * This is the REAL, production text-generation client. It sends the
 * fully-built prompt (produced by {@code PromptTemplate}) to OpenAI's
 * chat endpoint and turns the response into the plain
 * {@code LlmResponse} (text + token counts) the rest of the app expects.
 * <p>
 * Here's what {@code generate()} does, step by step:
 * <ol>
 *   <li>Build a small JSON request with the prompt as a single message.
 *       The "temperature" is set low (0.2), which makes the AI's answers
 *       more consistent and factual — appropriate for a policy assistant
 *       that shouldn't get "creative" with banking rules.</li>
 *   <li>Send it to OpenAI with our API key in the request header.</li>
 *   <li>If OpenAI responds with an error, throw
 *       {@code OpenAiApiException} with OpenAI's own error message
 *       attached.</li>
 *   <li>Otherwise, read the generated answer text out of the response,
 *       plus the real token counts OpenAI reports (used later for cost
 *       tracking).</li>
 * </ol>
 * <p>
 * Notice this class implements the exact same {@code LlmClient} interface
 * as {@code ExtractiveStubLlmClient} — that's what makes them
 * interchangeable. {@code RagAssistant} never needs to know or care which
 * one is actually running.
 * <p>
 * Just like {@code OpenAiEmbeddingModel}, the API key, base URL, and
 * model name all come from environment variables rather than being
 * hardcoded, and the constructor fails immediately with a clear error if
 * the key is missing — see {@code isConfigured()}, which callers check
 * FIRST so they can fall back to the offline stub instead.
 */
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
