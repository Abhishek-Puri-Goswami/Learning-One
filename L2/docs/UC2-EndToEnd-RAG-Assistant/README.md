# L2 / UC2 — End-to-End RAG Banking Assistant

Gen-AI Use Case submission. Builds directly on L2/UC1's Foundation & Core Retrieval (same corpus, same `rag-core` classes) to add generation, citations, guardrails, tracing, and evaluation — per the L2 HLD UseCase2 objective: "Build a fully functional RAG-based banking assistant that retrieves policy information, generates grounded responses with citations, and uses LangSmith for tracing, debugging, and evaluation."

## What's real vs. what's documented-but-unverified

Same split as UC1, and for the same reasons:

- **`rag-assistant-core/` was actually compiled and run.** It's a zero-dependency, pure-JDK module (no LangChain, no LangSmith SDK, no LLM API client) specifically so the full guardrail → retrieval → prompt → generation → citation → trace → evaluation pipeline could be executed for real in this sandbox, not just described. `SelfTests.java` (27 checks, hand-rolled — JUnit is unreachable via Maven Central here) passed 27/27 **after fixing two real bugs this run surfaced** (see "Real bugs found and fixed" below). `Main.java`'s demo ran 6 real queries through the real pipeline; the trace log and evaluation numbers in `reports/` are that run's actual, unedited output.
- **`rag-assistant-service/` (Spring Boot) could not be compile-verified** — same Maven Central block as every Spring Boot module in this submission. It reuses the already-verified `rag-assistant-core` classes rather than reimplementing anything.
- **LangChain and LangSmith themselves are not used.** Both are Python-first tools with no reachable Java dependency here. `PromptTemplate.java` is a hand-written equivalent of LangChain's prompt-template concept; `TraceLogger.java` is a hand-written, schema-aligned local stand-in for LangSmith (see `tracing/langsmith-tracing-design.md` for the exact field-by-field mapping and the production swap-in path).
- **The LLM itself is a disclosed, deterministic extractive stand-in** (`ExtractiveStubLlmClient.java`), not a real generative model — there is no reachable LLM API or key in this sandbox. It assembles answers only from sentences actually present in retrieved chunks, which makes the pipeline runnable and its citations trustworthy by construction, but its answer *quality* is not representative of a real LLM's — this is disclosed explicitly in `evaluation/rag-assistant-evaluation-summary.md`'s "Generation Quality" section rather than hidden behind clean-looking output.

## Real bugs found and fixed (not hypothetical — caught by this run)

1. **`PromptInjectionGuard`'s regex only matched a single qualifier word** (e.g. "ignore *the* instructions") and missed "ignore *all previous* instructions" — the exact phrasing used in the demo's own injection-attempt query. Caught by `SelfTests` failing on that case; fixed by allowing repeated qualifier words in the pattern.
2. **`EvaluationHarness`'s sentence-splitting regex split a citation marker away from the sentence it belonged to** (`"...rate. [chunk-id]"` → two fragments, neither faithfulness-checkable), causing a genuinely faithful answer to score 0.0. Caught by `SelfTests` failing on a hand-constructed faithful-answer case; fixed with a negative lookahead so `[chunk-id]` stays attached to its preceding sentence.

Both are documented with before/after reasoning directly in the fixed classes' code comments, not just here.

## Deliverables checklist (per L2 HLD UseCase2)

| Deliverable | Location | Status |
|---|---|---|
| Functional RAG pipeline with LangSmith integrated | `rag-assistant-core/.../assistant/RagAssistant.java`; REST wrapper: `rag-assistant-service/.../controller/AskController.java` | Built & run |
| Prompt + retriever configuration | `rag-assistant-core/.../assistant/PromptTemplate.java`; `prompts/prompt-template-design.md` | Built & run |
| Citation-enabled responses | `rag-assistant-core/.../assistant/CitationExtractor.java` | Built & run |
| Guardrail and fallback logic | `PromptInjectionGuard.java`, `UnsafeQueryGuard.java`, retrieval-guardrail logic in `RagAssistant.java`; `guardrails/guardrail-design.md` | Built & run — all 4 layers fired for real in the demo |
| LangSmith trace reports (retrieval, prompts, errors) | `TraceLogger.java`; `reports/langsmith-style-trace-log.jsonl`; `tracing/langsmith-tracing-design.md` | Built & run — 6 real trace records, JSON-validated |
| Short evaluation summary (faithfulness, relevance, latency) | `evaluation/rag-assistant-evaluation-summary.md` | Built — from real run data, includes a genuine guardrail tuning tension found in this run |

## Corpus

Same as UC1: `rag-assistant-core/corpus/*.txt`, 7 files of real text extracted from `Secure_Bank_policy_Manual_input-docs.pdf`.

## How to reproduce the real run

```bash
cd rag-assistant-core
mkdir -p out
javac -d out $(find src/main/java -name "*.java") $(find src/test/java -name "*.java")
java -cp out com.retailco.bankrag.assistant.SelfTests   # expect: 27 passed, 0 failed
java -cp out com.retailco.bankrag.assistant.Main corpus # expect: same output as reports/assistant-demo-run-log.txt
```

## Known limitations (disclosed, not hidden)

1. **Maven Central blocked** — `rag-assistant-service/` (Spring Boot) not compile-verified here; run `mvn clean verify` with real internet access before deployment.
2. **No real LLM** — `ExtractiveStubLlmClient` is extractive, not generative; see `evaluation/rag-assistant-evaluation-summary.md`'s "Generation Quality" section for a concrete example of where this shows up (a technically-faithful but not-directly-responsive answer).
3. **No real LangSmith** — `TraceLogger` is a schema-aligned local JSONL stand-in; see `tracing/langsmith-tracing-design.md`.
4. **Faithfulness/relevance are lexical-overlap proxies**, not LLM-judge scores — disclosed in `EvaluationHarness.java`'s Javadoc and `evaluation/rag-assistant-evaluation-summary.md`.
5. **The margin guardrail (0.03) both helped and over-blocked in the same 6-query run** — a real, load-bearing finding, not a limitation to paper over. See `evaluation/rag-assistant-evaluation-summary.md`'s "Tuning Tension" section, which hands a concrete recommendation to L2/UC4 (Intelligence Maturity & Optimization) rather than leaving it unresolved.
6. **PDF-extraction artifacts in corpus text** (stray `?` characters from bullet/special-character encoding, inherited unchanged from UC1's corpus files) occasionally appear in generated answers — visible in the complaint-escalation query's answer in `reports/assistant-demo-run-log.txt`.

## Tech stack

Java 17, plain JDK for `rag-assistant-core` (by design), Spring Boot 3.3.4 for `rag-assistant-service` — consistent with this submission's confirmed Java Spring Boot + React stack.
