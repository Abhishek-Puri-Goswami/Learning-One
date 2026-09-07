# Performance Optimization Summary

Deliverable: "Performance optimization summary," per L2 HLD UseCase4. Covers the three optimization levers this use case implements — caching, context-size budgeting, and similarity-threshold tuning — each backed by a real experiment in `reports/`, not a general essay about RAG performance.

## Lever 1: Caching (Experiment 1)

**What it does**: `QueryCache` (LRU, capacity-bounded, TTL-expiring) sits in front of `RagAssistant.ask()`. A repeated query — including a repeated guardrail-blocked or fallback query — is served from memory with zero retrieval, zero generation, zero token cost, and reported latency of 0ms.

**Real measured impact**: 3 of 7 demo requests (43%) were cache hits. See `cost/cost-estimation-document.md` for the direct dollar impact.

**Scope boundary, deliberately chosen**: caching sits only in front of L2/UC2's policy-retrieval pipeline, never in front of L2/UC3's `BankingToolService` live-data tools. Caching an account balance would silently return a stale figure to a customer — a correctness bug, not a performance win. This isn't an oversight; it's the reason `ObservableRagAssistant` wraps `RagAssistant` specifically and has no equivalent wrapper for `BankingToolService` anywhere in this submission.

**What would make this better in production**: TTL is currently a single fixed value (1 hour in the demo). A real deployment might vary TTL by query type — long TTL for stable policy facts (interest rate bands, required KYC documents), short or zero TTL for anything time-sensitive — though everything this use case's corpus covers (bank policy text) changes rarely enough that even a fairly long default TTL is a reasonable start.

## Lever 2: Context-size optimization (Experiment 2)

**What it does**: `ContextOptimizer` enforces a token budget across retrieved chunks, keeping the highest-scored chunks first (since `HybridSearcher`/`VectorStore` already return score-sorted results) and dropping — never truncating mid-chunk — whatever doesn't fit.

**Real measured impact**: retrieving 5 chunks for a real query totaled 791 tokens. At a 600-token budget, 3/5 chunks (540 tokens) were kept; at 400, 2/5 (360 tokens); at 200, 1/5 (180 tokens). This is a real, tunable trade-off surface, not a theoretical one — every number above came from an actual `HybridSearcher.search()` call against the real corpus.

**Trade-off, stated plainly**: a smaller context budget reduces prompt tokens (and therefore cost, per `cost/cost-estimation-document.md`) and can reduce hallucination surface area (less irrelevant context for a real LLM to be distracted by), but at the cost of losing potentially-relevant supporting chunks. **This module is not yet wired into `RagAssistant`'s actual prompt-building step** — it's demonstrated and tested standalone against real retrieved chunk sets, but `PromptTemplate.build()` (L2/UC2) still sends every retrieved chunk unconditionally. Wiring `ContextOptimizer` into that call site is a concrete, identified next step, not silently treated as already done.

## Lever 3: Similarity/margin threshold tuning (Experiment 3) — the highest-value finding in this use case

L2/UC2's `reports/rag-assistant-evaluation-summary.md` left an open question: its 0.03 margin guardrail correctly blocked one wrong answer but also incorrectly blocked one correct answer, in the same demo run, and explicitly handed the tuning question to this use case.

`ThresholdTuningExperiment` answers it with a real sweep over a 6-query labeled set (labels independently verified against the real corpus text — see the code comment in `ThresholdTuningExperiment.defaultLabeledQueries()` documenting a labeling mistake that was caught and corrected during this exercise, itself a real finding about how easy it is to get "ground truth" wrong):

| Margin threshold | Correctly answered | Correctly blocked | Incorrectly blocked | Incorrectly answered |
|---|---|---|---|---|
| 0.000 | 4 | 0 | 0 | 2 |
| 0.005 | 4 | 1 | 0 | 1 |
| **0.010** | **4** | **2** | **0** | **0** |
| **0.015** | **4** | **2** | **0** | **0** |
| 0.020 | 3 | 2 | 1 | 0 |
| 0.030 (UC2's original default) | 3 | 2 | 1 | 0 |
| 0.050 | 2 | 2 | 2 | 0 |
| 0.080 | 1 | 2 | 3 | 0 |

**At margin thresholds of 0.010–0.015, this 6-query sample achieves a perfect split: every query with a correct top chunk is answered, every query with a wrong top chunk is blocked, with zero errors in either direction.** UC2's original 0.03 default sits past the point where this sample's errors start reappearing (1 incorrectly-blocked query) — directly confirming UC2's finding and giving it a concrete number, not just a qualitative "this seems too strict."

**Recommendation**: lower the default `minScoreMargin` from 0.03 to **0.012** (the midpoint of the 0.010–0.015 zero-error range, giving a small safety margin on both sides) in `RagAssistant`'s configuration (`AssistantConfig`/`application.yml` in L2/UC2's `rag-assistant-service`).

**Caveat, stated as prominently as the finding**: 6 labeled queries is a small sample. This sweep demonstrates the *method* — real retrieval, real scores, real labeled ground truth, real counting — convincingly, and the specific 0.012 number is a genuinely evidence-based improvement over an arbitrary 0.03, but it should not be treated as final without testing against a substantially larger, more diverse query set (ideally hundreds of real or realistic queries covering every policy category) before being deployed as a hard production default.

## Cross-cutting observation

All three levers reduce cost and/or latency, but Lever 3 is qualitatively different: Levers 1 and 2 are pure efficiency gains (same correctness, less work), while Lever 3 is a correctness improvement with efficiency as a side effect (a correctly-lowered threshold means fewer needless fallbacks, which is both better UX and — per `cost/cost-estimation-document.md` — the same token/cost profile as any other answered query rather than a wasted retrieval-then-decline cycle). This is why Lever 3's finding is presented first among the three, despite being introduced last in L2 HLD UseCase4's Implementation Approach list.
