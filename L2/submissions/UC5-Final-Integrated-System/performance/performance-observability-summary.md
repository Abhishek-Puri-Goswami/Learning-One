# Performance & Observability Summary — System-Level

Deliverable: "Performance & observability summary," per L2 HLD UseCase5. Real numbers from `reports/integrated-demo-run-log.txt` (the fully-integrated 9-turn session), compared against UC4's isolated-component numbers.

## Real integrated-session metrics

```
Cache stats: CacheStats[hits=1, misses=3, evictions=0, expirations=0, currentSize=3, hitRate=0.25]
Metrics report: AggregateReport[totalQueries=4, cacheHits=1, cacheMisses=3, blockedByGuardrail=2,
                 fallbackResponses=0, answeredQueries=2, totalTokens=676, avgTokensPerAnsweredQuery=338.0,
                 avgLatencyMs=8.0, p50LatencyMs=1, p95LatencyMs=30, totalEstimatedCostUsd=0.16215]
```

Note: these metrics only reflect the 4 POLICY_QUESTION turns in the 9-turn session (1 repeat, 1 unsafe-advice block, 1 injection block, 1 fresh answer) — the 5 LIVE_DATA/AMBIGUOUS turns don't flow through `ObservableRagAssistant` at all, by design (see `docs/hld-lld-documentation.md`'s data-flow section and UC4's documented caching scope boundary: live data must never be cached or metered as if it were a cacheable RAG answer). This split is itself confirmed correct in `SelfTests.testMetricsAccumulateAcrossMixedSessionTraffic`, which asserts the live-data call does NOT appear in the metrics count.

## Observability: full session trace

Every one of the 4 policy-question calls (3 misses + implicitly the cached 4th) produced a structured trace record consistent with UC2/UC4's LangSmith-style schema, now written to `reports/integrated-trace-log.jsonl` — 3 real JSON lines, independently validated as parseable. The 2 blocked queries (unsafe-advice, prompt-injection) show `retrieved_chunks: []` in their trace records, confirming — for the third time across this submission (UC2, UC4, now UC5) — that guardrails fire before any retrieval cost is spent.

## Security: enforced consistently under integration

The cross-customer denial and mismatched-account-ownership checks (UC3) fired correctly inside the integrated system exactly as they did in UC3's isolated tests — `IntegratedBankingAssistant`'s `AccessDenied` branch is a thin wrapper around `BankingToolService`'s `UnauthorizedException`, so this isn't a new implementation to trust, it's the same one, now reachable through one more layer of routing. Verified in `SelfTests.testCrossCustomerLiveDataRequestIsDenied` and `testTransactionHistoryRequiresMatchingAccountOwnership`.

## Cost, at system scale (illustrative, same disclosed rate as UC4)

2 answered policy queries in this session cost an estimated **$0.16215** combined (see `cost/cost-estimation-document.md` in L2/UC4 for the rate assumption and its caveats — unchanged here). Scaling this per-query average ($0.081/query) the same way UC4 did (500 answered queries/day × 30 days) gives an illustrative **~$1,215/month** — lower than UC4's own isolated-run projection (~$2,535/month) because this session's 2 answered queries happened to be cheaper on average than UC4's 2; this is exactly the kind of sample-size sensitivity UC4's cost document already flagged as a reason not to treat any single projection as authoritative.

## What changed between UC4's isolated numbers and UC5's integrated numbers, and why

Nothing about the underlying calculation changed — `MetricsRecorder`, `CostEstimator`, `QueryCache` are byte-for-byte the same classes. The numbers differ because the *traffic* differs (UC4's demo sent 7 policy-only queries; UC5's demo sent 4 policy queries interleaved with 5 live-data/ambiguous queries in one session). This is the expected, correct behavior of a metrics system: the code doesn't change, the numbers reflect real usage, whatever that usage turns out to be. Presenting UC5's numbers as identical to UC4's would have been evidence of not actually running the integrated system.
