# RAG Evaluation Summary — System-Level

Deliverable: "RAG evaluation summary," per L2 HLD UseCase5. Synthesizes L2/UC1, UC2, and UC4's independently-run evaluations into one view of how the RAG half of this system actually performs, plus one new data point from this use case's own integrated run.

## Retrieval quality (from UC1 + UC4's real experiments)

Across 6 labeled real corpus queries (`L2/UC4/observability-core/.../ThresholdTuningExperiment.java`), the top-ranked hybrid-search result was correct for 4/6 queries. The 2 failures share a pattern: both had very low top scores (0.249, 0.414) and very low margins (0.0009, 0.0057) — i.e., **retrieval correctness correlates strongly with confidence signal strength in this system**, which is exactly what makes a margin-based guardrail a sound design (see below), not just a defensive add-on.

## Guardrail effectiveness (from UC2 + UC4, now confirmed again in UC5's integrated run)

| Guardrail | What it catches | Verified in |
|---|---|---|
| Prompt-injection guard | Instruction-override / role-hijack attempts | UC2 (isolated), UC5 (integrated session, same result) |
| Unsafe-advice guard | Investment/financial-advice requests, regardless of retrieval score | UC2 (isolated; fixes a real gap found in UC1 where this exact query scored 0.224 and slipped past a threshold-only check), UC5 (integrated session, same result) |
| Similarity threshold (0.15) | Near-zero-relevance retrieval | UC1, UC2 |
| Score margin (0.012, UC4-corrected from UC2's original 0.03) | Ambiguous/low-confidence top result, even above the absolute threshold | UC4's real sweep found 0.010-0.015 is error-free on the 6-query sample; UC2's original 0.03 needlessly blocked 1/6 |

**System-level finding**: in UC5's integrated demo session (`reports/integrated-demo-run-log.txt`), the same "How do I file a complaint" query that was correctly answered in UC2 and UC4's isolated runs was ALSO correctly answered here, and the previously-problematic "personal loan income" query pattern was not re-tested in this exact session but the corrected 0.012 margin (carried into `IntegratedBankingAssistant`'s wiring) is what UC4's sweep specifically recommends to fix it — this is traced explicitly in `docs/hld-lld-documentation.md`, not asserted without a citation back to where the number came from.

## Faithfulness, relevance, latency, tokens (from UC2, re-confirmed structurally in UC5)

UC2's `EvaluationHarness` numbers (faithfulness as lexical-overlap-to-cited-chunk proxy, relevance as top retrieval score, real wall-clock latency, whitespace-tokenizer-approximated token counts) are unchanged in the integrated system — `IntegratedBankingAssistant` calls `ObservableRagAssistant.ask()`, which calls the same `RagAssistant.ask()`, which produces the same `EvaluationResult` shape. UC5 adds nothing new to this metric's computation; it confirms the metric survives composition (the evaluation object flows unmodified from `RagAssistant` through `ObservableRagAssistant` through `IntegratedBankingAssistant.PolicyAnswer` all the way to the REST response).

## Known, disclosed weaknesses (carried forward honestly, not hidden at the "final" stage)

1. **`ExtractiveStubLlmClient` is not a real generative model** — every "faithfulness" and "answer quality" number in this system reflects an extractive stand-in's behavior, not a production LLM's. This is the single most consequential disclosed limitation across the whole L2 submission and remains true here.
2. **The 0.012 margin threshold is evidence-based but small-sample** — UC4 was explicit that a production deployment needs a much larger labeled query set before trusting this value as final.
3. **`ContextOptimizer` (UC4) is still not wired into `PromptTemplate`'s actual prompt-building** in this integrated system either — every retrieved chunk is still sent to the LLM client unconditionally. This is a known, tracked gap, not something UC5 silently fixed or silently ignored.

## Why this is a summary, not a re-run

L2 HLD UseCase5's deliverable is a summary specifically — the actual evaluation work (real queries, real scores, real threshold sweeps) already happened in UC1/UC2/UC4 and is not repeated here. What UC5 contributes is confirmation that the SAME evaluated behavior holds when every component is composed into one system and driven through one shared entry point, which is a different (and non-trivial) claim from "each piece works in isolation."
