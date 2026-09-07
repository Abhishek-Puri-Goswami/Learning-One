package com.retailco.bankrag.core;

/**
 * Swap point for a real embedding provider. Per L2's reference guide (3.1
 * "Same Embedding Model Requirement"): whatever implementation is used here
 * MUST be used consistently for both indexing chunks and embedding user
 * queries, or similarity scores become meaningless.
 *
 * Production implementations to swap in (not runnable in this sandbox --
 * see design/embedding-generation-module.md): OpenAI text-embedding-3-small,
 * Azure OpenAI embeddings, AWS Titan Embeddings, or a locally-hosted model
 * (e.g. BGE / all-MiniLM via LangChain4j's embeddings-all-minilm-l6-v2,
 * which runs ONNX locally with no external API calls).
 */
public interface EmbeddingModel {
    double[] embed(String text);

    int dimensions();
}
