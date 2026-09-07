# L2 / UC1 — Foundation & Core Retrieval (Banking Policy RAG)

Gen-AI Use Case submission. Domain: Secure Bank's Policy & Operations Manual (real 39-page source PDF, `Secure_Bank_policy_Manual_input-docs.pdf`, provided in `L2/_Reference-Docs/`). Scope, per the L2 HLD's UseCase1 functional requirements: build the foundational document-ingestion and retrieval pipeline a full RAG banking assistant will be built on top of in later L2 use cases.

## What's real vs. what's documented-but-unverified

This is the one Java module in the entire submission where the retrieval **logic itself** was actually compiled and executed against real data, not just written and reviewed:

- `rag-core/` is a **zero-dependency, pure-JDK module** (no Spring, no Maven Central artifact needed) — deliberately architected that way specifically to be runnable in this sandbox despite Maven Central (`repo.maven.apache.org`) being blocked (403/connection failure, confirmed repeatedly across every other module in this submission).
- It was compiled with plain `javac` and run with plain `java` — **zero errors**. A hand-rolled test harness (`SelfTests.java`, since JUnit itself is unreachable via Maven Central) passed **13/13 checks**. The retrieval demo (`Main.java`) ran 5 real queries against the real 7-document / 37-chunk corpus. Full raw output is captured in `reports/selftests-run-log.txt` and `reports/retrieval-demo-run-log.txt` — nothing in those two files was hand-edited after the run.
- `rag-service/` is the production-shaped Spring Boot REST wrapper around that same logic. Like every Spring Boot module in this submission, it could **not** be compiled/verified here (same Maven Central block) — see its `pom.xml` header comment for the exact command to run on a machine with real internet access before deployment. It reuses the already-verified `rag-core` classes (copied as source, see pom.xml's explanation) rather than reimplementing anything, so the parts most likely to have logic bugs were the parts actually tested.

## Deliverables checklist (per L2 HLD UseCase1)

| Deliverable | Location | Status |
|---|---|---|
| Document ingestion pipeline | `rag-core/src/main/java/.../DocumentLoader.java`, `Chunker.java`, `Main.java`; REST wrapper: `rag-service/.../IngestionController.java` | Built & run |
| Chunking configuration design | `design/chunking-configuration.md` | Documented, with real corpus-based rationale |
| Embedding generation module | `rag-core/.../EmbeddingModel.java`, `LocalHashingEmbeddingModel.java`; design notes: `design/embedding-generation-module.md` | Built & run (local stand-in); production swap-in path documented |
| Vector database schema | `design/vector-database-schema.sql`, `design/vector-database-schema.md` | Documented (PostgreSQL + pgvector) |
| Retrieval comparison summary (keyword vs semantic vs hybrid) | `reports/retrieval-comparison-summary.md` | Built & run — real findings, including a case where hybrid did NOT fully fix a keyword error |
| Hallucination risk analysis | `reports/hallucination-risk-analysis.md` | Built — analyzes a real threshold-discrimination failure found in the actual run |
| Production REST API wrapper | `rag-service/` (Spring Boot) | Written, matches `rag-core`'s verified logic; not compile-verified (Maven Central blocked) |

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

1. **Maven Central blocked in this sandbox** — `rag-service/` (Spring Boot) could not be `mvn compile`-verified. Run `mvn clean verify` on a machine with real internet access before first deployment.
2. **`LocalHashingEmbeddingModel` is not a production embedding model** — it's a feature-hashing bag-of-words stand-in used to make the pipeline actually runnable here. See `design/embedding-generation-module.md` for the swap-in options and `reports/hallucination-risk-analysis.md` for a concrete case where this limitation caused a real guardrail near-miss.
3. **Whitespace tokenization, not a real BPE tokenizer** — token counts in `ChunkingConfig`/`Chunker` are an approximation; see `design/chunking-configuration.md`.
4. **In-memory `VectorStore`, not pgvector** — `rag-core`'s store is a local/dev stand-in for the schema in `design/vector-database-schema.sql`; see `design/vector-database-schema.md` for why that's an acceptable foundation-stage choice.

## Tech stack

Java 17, plain JDK for `rag-core` (by design — no framework dependency), Spring Boot 3.3.4 for `rag-service`, PostgreSQL + pgvector as the target production vector store (per stack confirmation: Java Spring Boot for the backend across this submission).
