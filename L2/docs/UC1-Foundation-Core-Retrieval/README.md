# L2 / UC1 — Foundation & Core Retrieval (Banking Policy RAG)

Gen-AI Use Case submission. Domain: Secure Bank's Policy & Operations Manual (real 39-page source PDF, `Secure_Bank_policy_Manual_input-docs.pdf`, provided in `L2/_Reference-Docs/`). Scope, per the L2 HLD's UseCase1 functional requirements: build the foundational document-ingestion and retrieval pipeline a full RAG banking assistant will be built on top of in later L2 use cases.

## What's real vs. what's documented-but-unverified

This is the one Java module in the entire submission where the retrieval **logic itself** was actually compiled and executed against real data, not just written and reviewed:

- `rag-core/` is a **zero-dependency, pure-JDK module** (no Spring, no Maven Central artifact needed) — deliberately architected that way specifically to be runnable in this sandbox despite Maven Central (`repo.maven.apache.org`) being blocked (403/connection failure, confirmed repeatedly across every other module in this submission).
- It was compiled with plain `javac` and run with plain `java` — **zero errors**. A hand-rolled test harness (`SelfTests.java`, since JUnit itself is unreachable via Maven Central) passed **13/13 checks**. The retrieval demo (`Main.java`) ran 5 real queries against the real 7-document / 37-chunk corpus. Full raw output is captured in `reports/selftests-run-log.txt` and `reports/retrieval-demo-run-log.txt` — nothing in those two files was hand-edited after the run.
- `rag-service/` is the production-shaped Spring Boot REST wrapper around that same logic. It reuses the already-verified `rag-core` classes (copied as source, see pom.xml's explanation) rather than reimplementing anything.
- **OpenAI integration added and build-verified.** A real `OpenAiEmbeddingModel` (pure JDK, `java.net.http.HttpClient`, calls `/v1/embeddings`) now backs `EmbeddingModel` and is used automatically whenever `OPENAI_API_KEY` is set in the environment, falling back to `LocalHashingEmbeddingModel` otherwise — both `rag-core`'s `Main.java` and `rag-service`'s `RagCoreConfig` select between them with the same `OpenAiEmbeddingModel.isConfigured()` check. `rag-core` was recompiled with `javac`/self-tests re-run clean (13/13, offline-fallback path), and `rag-service` was `mvn compile`-verified with real Maven Central access — both are now confirmed to actually build, not just written and reviewed. **A live call was confirmed end-to-end against a real OpenAI-compatible endpoint** (configured via `OPENAI_BASE_URL`/`OPENAI_EMBEDDING_MODEL`, both read from the environment — no code change needed to point at a gateway other than `api.openai.com`): the pure-JDK demo produced real embeddings and retrieval results, and `rag-service`'s `POST /api/v1/rag/ingest` + `GET /api/v1/rag/search` endpoints were exercised live and returned real, non-stub search results with real similarity scores.

## Deliverables checklist (per L2 HLD UseCase1)

| Deliverable | Location | Status |
|---|---|---|
| Document ingestion pipeline | `rag-core/src/main/java/.../DocumentLoader.java`, `Chunker.java`, `Main.java`; REST wrapper: `rag-service/.../IngestionController.java` | Built & run |
| Chunking configuration design | `design/chunking-configuration.md` | Documented, with real corpus-based rationale |
| Embedding generation module | `rag-core/.../EmbeddingModel.java`, `OpenAiEmbeddingModel.java` (real OpenAI `/v1/embeddings`), `LocalHashingEmbeddingModel.java` (offline fallback); design notes: `design/embedding-generation-module.md` | Real OpenAI integration implemented & build-verified; offline stand-in remains the automatic fallback when no API key is set |
| Vector database schema | `design/vector-database-schema.sql`, `design/vector-database-schema.md` | Documented (PostgreSQL + pgvector) |
| Retrieval comparison summary (keyword vs semantic vs hybrid) | `reports/retrieval-comparison-summary.md` | Built & run — real findings, including a case where hybrid did NOT fully fix a keyword error |
| Hallucination risk analysis | `reports/hallucination-risk-analysis.md` | Built — analyzes a real threshold-discrimination failure found in the actual run |
| Production REST API wrapper | `rag-service/` (Spring Boot) | Written, matches `rag-core`'s verified logic; `mvn compile`-verified with real Maven |

## Corpus

`rag-core/corpus/*.txt` — 7 files, real text extracted from `Secure_Bank_policy_Manual_input-docs.pdf`: KYC & onboarding, account operations, loan processing, card issuance & fraud, information security, customer grievance, fixed deposit policy. This same corpus is what `rag-service`'s `POST /api/v1/rag/ingest` is designed to load (see `rag-service/API-EXAMPLES.md`).

## How to reproduce the real run

```bash
cd rag-core
mkdir -p out
javac -d out src/main/java/com/retailco/bankrag/core/*.java src/test/java/com/retailco/bankrag/core/*.java
java -cp out com.retailco.bankrag.core.SelfTests          # expect: 13 passed, 0 failed
java -cp out com.retailco.bankrag.core.Main corpus         # expect: same output as reports/retrieval-demo-run-log.txt
```

## Known limitations (disclosed, not hidden)

1. **`mvn compile` now verified** for `rag-service/` on a machine with real Maven Central access; run `mvn clean verify` before first deployment.
2. **`LocalHashingEmbeddingModel` remains the offline fallback**, used automatically whenever `OPENAI_API_KEY` is unset — it's a feature-hashing bag-of-words stand-in, not a production embedding model. See `design/embedding-generation-module.md` for details and `reports/hallucination-risk-analysis.md` for a concrete case where this limitation caused a real guardrail near-miss.
3. **Whitespace tokenization, not a real BPE tokenizer** — token counts in `ChunkingConfig`/`Chunker` are an approximation; see `design/chunking-configuration.md`.
4. **In-memory `VectorStore`, not pgvector** — `rag-core`'s store is a local/dev stand-in for the schema in `design/vector-database-schema.sql`; see `design/vector-database-schema.md` for why that's an acceptable foundation-stage choice.

## Tech stack

Java 17, plain JDK for `rag-core` (by design — no framework dependency), Spring Boot 3.3.4 for `rag-service`, PostgreSQL + pgvector as the target production vector store (per stack confirmation: Java Spring Boot for the backend across this submission).
