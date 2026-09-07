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
 * Real implementation of {@link LlmClient} backed by OpenAI's Chat
 * Completions API (https://api.openai.com/v1/chat/completions), called
 * directly over {@link HttpClient} (built into the JDK -- this module has
 * zero external dependencies by design, so no OpenAI SDK/Jackson/Gson is
 * pulled in; see {@code MinimalJson} for the small hand-rolled JSON layer).
 *
 * <p>The API key is read ONLY from the {@code OPENAI_API_KEY} environment
 * variable -- never hardcoded, logged, or written to any file. If it is
 * unset or blank, construction fails fast with {@link IllegalStateException}
 * so callers fall back to {@link ExtractiveStubLlmClient} instead (see each
 * project's {@code Main.java} / Spring {@code *Config.java} for that runtime
 * switch, driven by {@link #isConfigured()}).
 *
 * <p>The exact prompt passed to {@link #generate(String)} is sent verbatim
 * as a single user message, matching how {@code ExtractiveStubLlmClient}
 * consumes the fully-assembled prompt from {@code PromptTemplate} -- so
 * swapping between the stub and this real client requires no change to
 * {@code RagAssistant} or prompt-building logic.
 *
 * <p>This class was written and reviewed against OpenAI's documented
 * request/response shape, but the sandbox this project was developed in has
 * no network route to {@code api.openai.com} (only github.com and npm's
 * registry are reachable there), so a live call could not be executed or
 * verified from that environment. Verify it yourself by setting
 * {@code OPENAI_API_KEY} and running the demo/tests on a machine with real
 * internet access.
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
