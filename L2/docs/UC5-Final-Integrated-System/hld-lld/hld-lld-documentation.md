# HLD and LLD Documentation — Final Integrated Banking RAG System

Deliverable: "HLD and LLD documentation," per L2 HLD UseCase5's "Documentation finalization." This document synthesizes L2/UC1-UC4 into one system-level view rather than repeating each use case's own documentation — see each use case's own `README.md`/design docs for component-level detail this document intentionally doesn't duplicate.

## High-Level Design

### System purpose

A single AI-powered banking support system that answers two categories of customer question through one entry point: policy questions (grounded, cited, retrieved from Secure Bank's official policy manual) and live account questions (balance, transactions, loan outstanding — retrieved securely from backend data, never generated).

### High-level architecture

```
Customer
   │
   ▼
SupportController (POST /api/v1/support/ask)
   │
   ▼
IntentClassifier ─────────┬─────────────────────┐
   │                      │                      │
   ▼                      ▼                      ▼
POLICY_QUESTION      LIVE_DATA               AMBIGUOUS
   │                 (balance/txn/loan)           │
   ▼                      │                       ▼
ObservableRagAssistant    ▼                  clarification
   │                 BankingToolService       response
   ▼                      │
RagAssistant               ▼
(guardrails→retrieval      JwtService (auth) + PiiMasking
 →prompt→generation→            │
 citation→trace→eval)           ▼
   │                       BankingDataStore
   ▼
VectorStore (policy corpus)
```

### Non-functional requirements addressed

| Requirement | How it's met |
|---|---|
| Factual accuracy | RAG grounding + inline citations (UC2); citation resolution check flags any unresolvable citation |
| Hallucination minimization | Similarity + margin retrieval guardrail (UC1/UC2), data-tuned in UC4; unsafe-advice guard routes financial-advice questions away from generation entirely |
| Security | JWT authentication + per-request authorization (UC3); PII masking on every live-data field |
| Performance | Query caching, context-size budgeting (UC4) |
| Observability | Structured LangSmith-style tracing (UC2), aggregate metrics + cost estimation (UC4) |
| Extensibility | Every component depends on an interface (`EmbeddingModel`, `LlmClient`) or is independently swappable (config-driven weights/thresholds), documented per use case |

## Low-Level Design

### Module inventory (all in `banking-support-core`/`banking-support-service`, sourced from their originating use case)

| Package | Classes | Origin |
|---|---|---|
| `com.retailco.bankrag.core` | `Chunk`, `Chunker`, `ChunkingConfig`, `DocumentLoader`, `EmbeddingModel`, `LocalHashingEmbeddingModel`, `VectorStore`, `KeywordSearcher`, `HybridSearcher`, `ScoredChunk` | L2/UC1 |
| `com.retailco.bankrag.assistant` | `PromptTemplate`, `PromptInjectionGuard`, `UnsafeQueryGuard`, `LlmClient`, `ExtractiveStubLlmClient`, `CitationExtractor`, `TraceLogger`, `EvaluationHarness`, `RagAssistant` | L2/UC2 |
| `com.retailco.bankrag.security` | `JwtService`, `PiiMasking`, `BankingDataStore`, `BankingToolService`, `IntentClassifier`, `UnauthorizedException` | L2/UC3 |
| `com.retailco.bankrag.observability` | `QueryCache`, `MetricsRecorder`, `CostEstimator`, `ContextOptimizer`, `ObservableRagAssistant`, `ThresholdTuningExperiment` (analysis-only, not runtime) | L2/UC4 |
| `com.retailco.bankrag.integration` | `IntegratedBankingAssistant` | L2/UC5 (this use case) — the only NEW class; every other class is reused unchanged |

### `IntegratedBankingAssistant.handle()` — the one new integration point

```java
public UnifiedResponse handle(String query, String bearerToken, String requestedCustomerId, String accountNumber) {
    Intent intent = intentClassifier.classify(query);
    return switch (intent) {
        case POLICY_QUESTION -> new PolicyAnswer(observableRagAssistant.ask(query));
        case ACCOUNT_BALANCE, TRANSACTION_HISTORY, LOAN_OUTSTANDING ->
            /* delegate to bankingToolService, catch UnauthorizedException -> AccessDenied */;
        case AMBIGUOUS -> new Ambiguous(...);
    };
}
```

No retrieval, generation, authentication, or masking logic lives in this class — it is purely a router and response-shape unifier, by design (see `banking-support-core`'s `IntegratedBankingAssistant.java` Javadoc). This keeps every use case's independently-verified logic exactly as verified; UC5 adds zero new business rules to get wrong.

### Data flow for a LIVE_DATA request (LLD detail)

1. `SupportController` reads the raw `Authorization` header (unparsed) and passes it straight through — no token handling in the controller layer.
2. `BankingToolService.verifyAndAuthorize()` (UC3, unchanged) calls `JwtService.verify()` (real HMAC-SHA256 verification), checks `claims.subject().equals(requestedCustomerId) || claims.roles().contains("ADMIN")`.
3. On success, `BankingDataStore` (in-memory, UC3) returns raw records; `PiiMasking` (UC3) transforms them before they leave `BankingToolService`.
4. `SupportController` wraps the masked result in `AskResponse("LIVE_DATA", ...)` — the masking has already happened two layers down; the controller never sees raw PII.

### Data flow for a POLICY_QUESTION request (LLD detail)

1. `ObservableRagAssistant.ask()` (UC4) checks `QueryCache` first (normalized-query key).
2. On a miss, delegates to `RagAssistant.ask()` (UC2, unchanged): guardrails → `HybridSearcher` (UC1) → retrieval guardrail (threshold 0.15 + margin 0.012, UC4-corrected) → `PromptTemplate` → `ExtractiveStubLlmClient` → `CitationExtractor` → `TraceLogger` (writes to `reports/integrated-trace-log.jsonl`) → `EvaluationHarness`.
3. `ObservableRagAssistant` records latency/tokens/cost into `MetricsRecorder`, caches the result, returns.

## What UC5 validated that no prior use case could validate alone

Every prior use case's `SelfTests.java` tests its own components in isolation (with hand-built minimal fixtures, e.g. UC4's `SelfTests` builds a 3-chunk `VectorStore` by hand rather than loading the real corpus for most tests). **`banking-support-core`'s `SelfTests.java` is the first test suite in this submission that calls the real, fully-wired system** — `IntentClassifier` routing into whichever of `ObservableRagAssistant` or `BankingToolService` is correct, using the real corpus and real JWT tokens, and asserts on the outcome type (`PolicyAnswer`/`LiveDataAnswer`/`AccessDenied`/`Ambiguous`), not just on an individual class's return value. 14/14 passed — see `reports/integrated-selftests-run-log.txt`.
