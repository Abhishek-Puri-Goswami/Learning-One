# Embedding Generation Module — Design Notes

Deliverable: "Embedding generation module."

## Interface (production-shaped, already implemented)

```java
public interface EmbeddingModel {
    double[] embed(String text);
    int dimensions();
}
```

`rag-core/src/main/java/.../EmbeddingModel.java`. Every other component (`VectorStore`, `HybridSearcher`) depends only on this interface, never on a concrete model — swapping the embedding provider is a one-line change at the call site (`new LocalHashingEmbeddingModel(256)` → `new OpenAiEmbeddingModel(...)`), consistent with L2's reference guide §3.1 rule that the SAME model must be used for indexing and querying (enforced structurally here: both `VectorStore.index()` and `VectorStore.semanticSearch()` go through the one injected `EmbeddingModel` instance).

## Reference Implementation Shipped: `LocalHashingEmbeddingModel`

This sandbox has no reachable embedding API (no network egress to OpenAI/Azure/AWS) and no reachable Maven Central to pull a local ONNX model (e.g., LangChain4j's `all-MiniLM-L6-v2`). To make the retrieval pipeline actually runnable and testable here (see `reports/retrieval-comparison-summary.md` for real, executed output), `LocalHashingEmbeddingModel` implements the interface using the **hashing trick**: term-frequency counts of whitespace tokens hashed into a fixed-size vector, L2-normalized.

**This is explicitly NOT a production embedding model.** It captures literal term overlap reasonably well (enough to demonstrate and test ranking, thresholding, and the retrieval-comparison pipeline end-to-end) but does not capture true semantic similarity — it cannot tell that "premature withdrawal" and "early closure" mean the same thing the way a trained embedding model would. See `reports/hallucination-risk-analysis.md` for a concrete example where this limitation shows up in the actual demo run.

## Production Swap-In Options (not runnable in this sandbox)

| Option | When to use | Notes |
|---|---|---|
| OpenAI `text-embedding-3-small` | Fastest path to production quality | Requires API key + network egress; 1536 dimensions (matches `vector-database-schema.sql`) |
| Azure OpenAI embeddings | If Secure Bank's compliance posture requires Azure's data-residency/BAA terms | Same model family, different endpoint/auth |
| AWS Titan Embeddings | If already on AWS | Different dimensionality — update `vector-database-schema.sql`'s `VECTOR(1536)` accordingly |
| Local BGE / `all-MiniLM-L6-v2` via LangChain4j | On-prem / air-gapped deployment requirement (banking data never leaves the network) | `langchain4j-embeddings-all-minilm-l6-v2` runs ONNX locally, no external API calls once the model artifact is downloaded — the best long-term fit for a bank that cannot send policy text to a third-party API at all |

## Module Boundaries (matches `rag-service`'s intended Spring Boot wiring)

```
IngestionController → DocumentLoader → Chunker → EmbeddingModel → VectorStore
SearchController     → EmbeddingModel (same instance/config) → VectorStore.semanticSearch()
```

`rag-service/` wires this as a Spring `@Configuration` bean (`EmbeddingModelConfig`) so the concrete implementation is chosen once, centrally, by profile (`local` → hashing stand-in for dev/demo, `prod` → real provider) — never instantiated ad hoc inside a controller or service class.
