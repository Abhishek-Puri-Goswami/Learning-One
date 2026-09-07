# Observability Metrics Report

Deliverable: "Observability metrics report," per L2 HLD UseCase4. All numbers below are real output from `reports/observability-demo-run-log.txt` (Experiment 1: `ObservableRagAssistant` over 7 real queries against the real Secure Bank policy corpus).

## Traffic pattern and cache behavior

7 queries were sent, deliberately including 3 repeats (2 legitimate repeated questions, 1 repeated guardrail-blocked question) to model realistic FAQ-style traffic where many customers ask the same handful of questions:

```
CacheStats[hits=3, misses=4, evictions=0, expirations=0, currentSize=4, hitRate=0.4286]
```

3 of 7 requests (43%) were served from cache with zero additional retrieval, generation, or token cost — including the repeated guardrail-blocked query ("Should I invest my savings in mutual funds right now?"), which is deliberately cached too: a repeated unsafe-advice attempt shouldn't re-run the guardrail check and produce a fresh trace record any more than a repeated legitimate question should re-run retrieval. See `performance/performance-optimization-summary.md` for the cost/latency impact of this.

## Query outcome breakdown

```
AggregateReport[totalQueries=7, cacheHits=3, cacheMisses=4, blockedByGuardrail=2, fallbackResponses=2, answeredQueries=3, ...]
```

Both `blockedByGuardrail=2` and `fallbackResponses=2` count across all 7 requests including cache hits (a cached blocked/fallback response is still counted as such when its metric is recorded, per `ObservableRagAssistant.ask()`'s cache-hit branch) — traced back to the 4 distinct queries via `reports/observability-trace-log.jsonl`:

| Query | Outcome |
|---|---|
| "How do I file a complaint..." | Answered (both the miss and its later cache hit) |
| "What is the minimum monthly income required for a personal loan?" | **Fallback** (both calls) — the retrieval margin guardrail fired: this run used the default 0.03 margin threshold, and this exact query's real margin is 0.0155, below it |
| "What documents are required for KYC verification?" | Answered |
| "Should I invest my savings in mutual funds..." | **Blocked** (both calls) — unsafe-advice guardrail |

The personal-loan-income fallback here is the SAME false-negative Experiment 3 (`observability/../reports` — see `cost/cost-estimation-document.md`'s sibling doc `performance/performance-optimization-summary.md`) independently found and quantified through the margin-threshold sweep: at the 0.03 default, this legitimate, correctly-answerable question is needlessly declined. Experiment 1 shows this happening in a live-traffic-shaped run; Experiment 3 shows exactly why (margin 0.0155 < 0.03) and what threshold would fix it (0.010–0.015). Neither experiment was designed to agree with the other — they did, because both are measuring the real, same underlying pipeline.

## Latency

```
avgLatencyMs=12.0, p50LatencyMs=0, p95LatencyMs=50
```

`p50=0` reflects that most requests in this small sample were either cache hits (0ms by definition) or fast local-computation misses — real numbers, but not representative of production latency any more than L2/UC2's sub-21ms latencies were (see that use case's evaluation summary for the same disclosed caveat: `ExtractiveStubLlmClient` does local string processing, not a network call to a real LLM API). What IS representative and reusable in production: the measurement mechanism itself (wall-clock timing around the whole `ask()` call, correctly excluding cache hits from doing any pipeline work) — swapping in a real `LlmClient` would make these numbers production-meaningful without changing how or where they're measured.

## Token usage

```
totalTokens=1408, avgTokensPerAnsweredQuery=469.33
```

Only counted for cache-miss, non-blocked, non-fallback queries (a blocked or fallback query never reaches generation, so it correctly contributes 0 tokens — see `RagAssistant.blockedResponse`/the fallback branch in `RagAssistant.ask()` from L2/UC2, both of which set `promptTokens=0, completionTokens=0`). See `cost/cost-estimation-document.md` for what this converts to in estimated dollars.

## What this report does NOT claim

These are real numbers from a real 7-query run in this sandbox, not projections. `performance/performance-optimization-summary.md` extrapolates from these real per-query numbers to production-scale estimates, with that extrapolation clearly labeled as such.
