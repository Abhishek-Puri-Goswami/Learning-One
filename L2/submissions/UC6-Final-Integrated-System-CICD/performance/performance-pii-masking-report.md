# Performance & PII-Masking Report — L2/UC6

Deliverable: "Performance & PII-masking report," per L2 HLD UseCase6 (LLD 8.1).

## Performance

RAG-side performance (cache hit rate, latency percentiles, token/cost estimates) is unchanged from `L2/UC5-Final-Integrated-System/performance/performance-observability-summary.md` — UC6 adds RBAC and logging around `BankingToolService`'s live-data path, which UC4's `MetricsRecorder`/`QueryCache`/`CostEstimator` never instrumented in the first place (by design — see UC5's "only policy-question turns are metered" finding). Re-running the same 10-turn demo (now with an added SUPPORT_AGENT scenario) confirms this held:

```
Cache stats: CacheStats[hits=1, misses=3, evictions=0, expirations=0, currentSize=3, hitRate=0.25]
Metrics report: AggregateReport[totalQueries=4, cacheHits=1, cacheMisses=3, blockedByGuardrail=2,
                 fallbackResponses=0, answeredQueries=2, totalTokens=676, avgTokensPerAnsweredQuery=338.0,
                 avgLatencyMs=11.5, p50LatencyMs=0, p95LatencyMs=46, totalEstimatedCostUsd=0.16215]
```

(Latency figures are real wall-clock measurements and vary a few milliseconds between runs on this shared sandbox -- `totalEstimatedCostUsd` and every count field do not, since those are deterministic given the same corpus and query set.)

Identical `totalQueries=4` and `answeredQueries=2` to UC5's run (the RBAC-only turn added — SUPPORT_AGENT reading CUST1002's balance — is a LIVE_DATA call, correctly excluded from these metrics, same as every other LIVE_DATA call). `totalEstimatedCostUsd` is unchanged to the cent, confirming the two answered policy queries and their token counts are byte-for-byte the same as UC5 — nothing about the RAG pipeline shifted.

### New performance surface: the audit log itself

`StructuredAuditLogger.log()` performs one synchronous file append per access decision (`Files.writeString(..., APPEND)`), so it adds real (if small) latency to every `BankingToolService` call — not reflected in `MetricsRecorder` (that class only instruments the RAG path, unchanged). This is a disclosed, not hidden, design gap: a production system would want the audit write to be async / batched so a slow disk never blocks a live-data response, and would want its own latency metric. Out of scope for this submission's illustrative implementation, tracked here rather than silently ignored.

## PII masking — audit of what actually left the system, end to end

This section is what makes "PII-masking report" more than a restatement of UC3's masking rules: it traces the real 10-turn demo run's actual output and actual audit log, field by field, and confirms nothing unmasked reached either.

### 1. Response-level masking (UC3, re-confirmed here)

From `banking-support-core/reports/integrated-demo-run-log.txt`'s real output:

```
[LIVE DATA: ACCOUNT_BALANCE] [MaskedAccount[accountNumberMasked=XXXXXXXX0456, ...
[LIVE DATA: ACCOUNT_BALANCE] [MaskedAccount[accountNumberMasked=XXXXXXXX0999, ...   <- SUPPORT_AGENT turn
```

Both the CUSTOMER's own balance query and the SUPPORT_AGENT's elevated cross-customer query returned masked account numbers — RBAC-elevated access does not bypass masking, because masking happens inside `BankingToolService` after the `AccessPolicy` check, structurally before any value can leave the method (see `docs/secure-backend-integration-design.md`'s RBAC section). This is independently asserted, not just observed, in `RbacAndAuditSelfTests.testSupportAgentReceivedDataIsStillMasked`.

### 2. Audit-log-level masking (new in UC6)

From the real `banking-support-core/reports/audit-log.jsonl` produced by that same run:

```json
{"timestamp":"2026-08-30T20:13:34.855293743Z","correlationId":"ddd4804d-471b-4f36-b1ec-35cfc8b07aa3","event":"banking_tool_access","actorSubject":"CUST1001","actorRoles":["CUSTOMER"],"requestedCustomerId":"CUST1001","tool":"get_account_balance","decision":"ALLOWED_SELF","reason":"subject matches requested customer"}
{"timestamp":"2026-08-30T20:13:34.928300533Z","correlationId":"50664120-d77d-4275-974a-878db183655b","event":"banking_tool_access","actorSubject":"CUST1001","actorRoles":["CUSTOMER"],"requestedCustomerId":"CUST1002","tool":"get_account_balance","decision":"DENIED_AUTHORIZATION","reason":"subject 'CUST1001' holds no role that permits access to customer 'CUST1002'"}
{"timestamp":"2026-08-30T20:13:34.929623052Z","correlationId":"82dab752-3087-4828-954a-e48c2041d15c","event":"banking_tool_access","actorSubject":"AGENT-042","actorRoles":["SUPPORT_AGENT"],"requestedCustomerId":"CUST1002","tool":"get_account_balance","decision":"ALLOWED_ELEVATED","reason":"SUPPORT_AGENT role grants read-only cross-customer access"}
```

(Real output from this submission's own run — `banking-support-core/reports/audit-log.jsonl` — not illustrative sample data.)

Every field is decision metadata (who, what role, which customer id, which tool, allow/deny, why) — none is an account number, balance, transaction amount, mobile number, or government ID. This is verified programmatically, not just eyeballed: `RbacAndAuditSelfTests.testAuditLogNeverContainsRawAccountNumber` fetches a real account's raw (unmasked) account number directly from `BankingDataStore` and asserts the string never appears anywhere in the audit log file, after a real `BankingToolService` call that touched that exact account.

### 3. HTTP-access-log-level masking (service module, not run in this sandbox)

`HttpAccessLogFilter` (service module only) logs method/path/status/duration/correlation-id and nothing else — no headers, no body — by construction (it has no code path that reads either). Not executed here (same Maven Central limitation as the rest of `banking-support-service/`); its masking guarantee is structural (no field it captures could ever contain PII, unlike `StructuredAuditLogger` which had to be tested against a real record to rule it out) rather than test-verified, and disclosed as such.

## Summary table

| Layer | Masking mechanism | Verified how |
|---|---|---|
| API response body | `PiiMasking` (UC3) | Real demo run output + `RbacAndAuditSelfTests` |
| Structured audit log | Field allowlist (UC6) | Real demo run's `audit-log.jsonl` + a programmatic "never contains the raw value" test |
| HTTP access log | Field allowlist by construction (UC6) | Structural (no PII-capable field exists) — not runtime-tested (Spring module not compile-verified here) |
