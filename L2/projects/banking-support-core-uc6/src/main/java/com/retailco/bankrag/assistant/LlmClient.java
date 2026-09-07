package com.retailco.bankrag.assistant;

/**
 * Abstraction over the generation model, mirroring how rag-core's
 * EmbeddingModel interface decouples retrieval from a specific embedding
 * provider (see L2/UC1's design/embedding-generation-module.md for the same
 * pattern). Every other component (RagAssistant, EvaluationHarness) depends
 * only on this interface -- swapping in a real LLM provider (OpenAI, Azure
 * OpenAI, Anthropic, Bedrock) is a one-line change in AssistantConfig, never
 * a change to orchestration logic.
 */
public interface LlmClient {

    record LlmResponse(String rawText, int estimatedPromptTokens, int estimatedCompletionTokens) {
    }

    LlmResponse generate(String prompt);

    String modelId();
}
