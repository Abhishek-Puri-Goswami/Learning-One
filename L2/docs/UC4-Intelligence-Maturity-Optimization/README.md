# L2 / UC4 — Intelligence Maturity & Optimization

Gen-AI Use Case submission. Objective, per L2 HLD UseCase4: "Improve system maturity through performance tracking, cost control, and observability." Builds directly on L2/UC2's `RagAssistant` pipeline, adding an observability layer (caching, metrics, cost estimation, context optimization) on top without modifying UC2's already-verified internals — and closes an open question UC2 explicitly left for this use case to answer.

## Closing the loop from L2/UC2

L2/UC2's `reports/rag-assistant-evaluation-summary.md` found that its 0.03 margin guardrail correctly blocked one wrong-chunk answer but also incorrectly blocked one correct answer, in the same real demo run, and recommended: *"consider a category-aware or corpus-density-aware margin... informed directly by real evidence rather than chosen a priori."*

This use case's `ThresholdTuningExperiment` does exactly that: a real sweep over a 6-query labeled set found that **margin thresholds of 0.010–0.015 achieve a perfect split** on this sample (0 false blocks, 0 false answers), while UC2's original 0.03 default sits past the point where errors reappear. `observability-service`'s `application.yml` ships the corrected default (**0.012**), with the finding, the caveat about sample size, and the full sweep table in `performance/performance-optimization-summary.md`.

## What's real vs. what's documented-but-unverified

Same split as every prior L2 use case, for the same reason (Maven Central blocked in this sandbox):

- **`observability-core/` was actually compiled and run.** `SelfTests.java` (26 checks, hand-rolled) passed 26/26. `Main.java` ran three real experiments: (1) `ObservableRagAssistant` over 7 queries with real cache hits/misses and real cost numbers, (2) `ContextOptimizer` token-budget trimming against real retrieved chunks, (3) the threshold-tuning sweep above. Every number in `reports/` is that run's actual, unedited output.
- **A real labeling mistake was caught and fixed during this exercise**: two of the six labeled "correct chunk" ground-truth values in `ThresholdTuningExperiment` were initially wrong (assumed the answer would be in each document's first chunk; the real corpus text showed both were generic front-matter). This was caught by manually walking the real chunk boundaries against the raw corpus text before trusting the experiment's conclusions — documented in the code comment and in `performance/performance-optimization-summary.md`, not quietly corrected without a trace.
- **`observability-service/` (Spring Boot) is now `mvn compile`-verified** with real Maven Central access. It reuses `observability-core`'s already-verified `QueryCache`/`MetricsRecorder`/`CostEstimator`/`ContextOptimizer`/`ObservableRagAssistant` classes unchanged.
- **OpenAI integration added and build-verified.** The `RagAssistant` wrapped by `ObservableRagAssistant` now uses a real `OpenAiEmbeddingModel`/`OpenAiLlmClient` whenever `OPENAI_API_KEY` is set (both `Main.java` and `observability-service`'s `ObservabilityConfig` select between real and offline stand-ins with the same `isConfigured()` check), falling back to `LocalHashingEmbeddingModel`/`ExtractiveStubLlmClient` otherwise. `observability-core` recompiled and self-tests re-run clean (26/26, offline-fallback path); `observability-service` `mvn compile`-verified. A live call against a real OpenAI-compatible endpoint was confirmed end-to-end in the sibling UC1/UC2 modules (`rag-service`/`rag-assistant-core`), which share this exact same client code (`OpenAiEmbeddingModel`/`OpenAiLlmClient`); this module's own live path was not independently re-run.

## Deliverables checklist (per L2 HLD UseCase4)

| Deliverable | Location | Status |
|---|---|---|
| Observability metrics report | `observability/observability-metrics-report.md` | Built — from real run data, traced to exact per-query outcomes |
| Token usage summary | Embedded in `observability/observability-metrics-report.md` and `cost/cost-estimation-document.md` | Built — real token counts |
| Cost estimation document | `cost/cost-estimation-document.md` | Built — real token counts × a disclosed-as-illustrative rate, with a concrete precision-improvement noted |
| Performance optimization summary | `performance/performance-optimization-summary.md` | Built — 3 real experiments, including the threshold-tuning finding above |
| Agent reasoning concept note | `agent-reasoning/agent-reasoning-concept-note.md` | Built — explicitly conceptual per the HLD, grounded in this submission's actual code seams |
| Token usage tracking / Latency monitoring | `observability-core/.../observability/MetricsRecorder.java` | Built & run |
| Caching strategy | `QueryCache.java` | Built & run |
| Context optimization | `ContextOptimizer.java` | Built & run (standalone; not yet wired into `PromptTemplate` — disclosed in the performance summary) |
| Cost estimation (code) | `CostEstimator.java` | Built & run |
| Tune similarity threshold | `ThresholdTuningExperiment.java` | Built & run — the headline finding |

## How to reproduce the real run

```bash
cd observability-core
mkdir -p out
javac -d out $(find src/main/java -name "*.java") $(find src/test/java -name "*.java")
java -cp out com.retailco.bankrag.observability.SelfTests   # expect: 26 passed, 0 failed
java -cp out com.retailco.bankrag.observability.Main corpus # expect: same output as reports/observability-demo-run-log.txt
```

## Known limitations (disclosed, not hidden)

1. **`mvn compile` now verified** for `observability-service/` on a machine with real Maven Central access; run `mvn clean verify` before deployment.
2. **Cost rate is illustrative**, not a live-priced API quote — see `cost/cost-estimation-document.md`.
3. **Cost's prompt/completion split is approximated (80/20)**, not exact — a concrete, identified fix (extend `AssistantResponse` to carry the real split) is documented rather than silently left as a permanent gap.
4. **`ContextOptimizer` is not yet wired into `RagAssistant`'s actual prompt-building** — demonstrated and tested standalone against real retrieved chunks; the integration point is identified in `performance/performance-optimization-summary.md`.
5. **The 0.012 recommended margin threshold comes from a 6-query labeled sample** — a real, methodologically sound result, but explicitly flagged as needing a much larger query set before being treated as a final production value.
6. **Caching is scoped only to L2/UC2's policy-retrieval pipeline**, deliberately never to L2/UC3's live banking-data tools — see `performance/performance-optimization-summary.md` for why that boundary is a correctness requirement, not an oversight.

## Tech stack

Java 17, plain JDK for `observability-core` (by design), Spring Boot 3.3.4 for `observability-service` — consistent with this submission's confirmed Java Spring Boot + React stack.
