package com.retailco.bankrag.assistant;

// CONCEPT: Strategy pattern (same idea as core's EmbeddingModel interface).
// PURPOSE: Defines the one contract every "answer generator" must satisfy:
// take a fully-built prompt string, return generated text plus rough token
// counts. RagAssistant (the orchestrator) and EvaluationHarness depend only
// on this interface, never on a concrete implementation.
// WHY: this is what lets ExtractiveStubLlmClient (offline stand-in) and
// OpenAiLlmClient (real generative model) be completely interchangeable --
// swapping between them is a one-line change in AssistantConfig's
// @Bean method, with zero change to RagAssistant's orchestration logic.
// IMPORTANT: `LlmResponse` is a nested record -- a small immutable data
// holder scoped inside the interface because it only ever makes sense in
// the context of "a response from an LlmClient."
public interface LlmClient {

    record LlmResponse(String rawText, int estimatedPromptTokens, int estimatedCompletionTokens) {
    }

    LlmResponse generate(String prompt);

    String modelId();
}
