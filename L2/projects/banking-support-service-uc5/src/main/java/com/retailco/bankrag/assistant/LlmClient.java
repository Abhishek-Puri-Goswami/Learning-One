package com.retailco.bankrag.assistant;

/**
 * This interface defines the ONE thing any "answer generator" must be
 * able to do: take a fully-built prompt and return the generated answer
 * text, along with a rough token count. {@code RagAssistant} (the class
 * that coordinates the whole pipeline) and {@code EvaluationHarness}
 * depend only on this interface, never on a specific implementation.
 * <p>
 * That's exactly what lets {@code ExtractiveStubLlmClient} (our offline
 * fallback) and {@code OpenAiLlmClient} (the real AI model) be
 * completely interchangeable — switching between them is a one-line
 * change in a Spring configuration class, with zero change to how
 * {@code RagAssistant} works.
 * <p>
 * {@code LlmResponse} is declared right inside this interface as a
 * nested record, since it only ever makes sense in the context of "a
 * response that came from an LlmClient."
 */
public interface LlmClient {

    record LlmResponse(String rawText, int estimatedPromptTokens, int estimatedCompletionTokens) {
    }

    LlmResponse generate(String prompt);

    String modelId();
}
