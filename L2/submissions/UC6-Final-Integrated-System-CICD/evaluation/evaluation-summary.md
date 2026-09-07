# Evaluation Summary — L2/UC6

Deliverable: "Evaluation summary (accuracy, retrieval quality, latency)," per L2 HLD UseCase6 (LLD 8.1) and the LLD's Functional Scope item "LangSmith-powered trace analysis & evaluation."

This use case does not re-run UC1/UC2/UC4's retrieval and guardrail evaluations — see `L2/UC5-Final-Integrated-System/evaluation/rag-evaluation-summary.md` for that full synthesis, unchanged and still accurate here (UC6 adds RBAC and CI/CD around the same RAG pipeline; it does not touch `HybridSearcher`, `RagAssistant`, or the 0.012 margin threshold). This document covers only what's new: whether the RAG pipeline's behavior survives composition with RBAC and structured logging, and what the CI/CD "evaluation stage" concretely validates.

## What the `evaluation` CI job actually checks

Per LLD 6.1's "LangSmith evaluation stage" requirement, the `evaluation` job in `.github/workflows/ci-cd.yml`:

1. Runs `banking-support-core`'s full integrated demo (`Main.java`) — the same 10-turn mixed session described in `performance/performance-pii-masking-report.md`.
2. Parses `banking-support-core/reports/integrated-trace-log.jsonl` with Python's `json.loads` on every line, failing the build if any trace record is malformed.

This was run for real in this sandbox (not just described): **3 trace records, all valid JSON** — see `banking-support-core/reports/` for the exact log. A production LangSmith integration would replace step 2 with an actual API call pushing these traces to LangSmith's platform for cross-run comparison; that integration itself needs a reachable LangSmith API key/endpoint this sandbox doesn't have, which is why `TraceLogger` remains the same disclosed local-JSONL stand-in UC2 introduced (see UC2's own evaluation docs for the schema-alignment reasoning).

## RAG behavior confirmed unchanged under RBAC + logging

Running `banking-support-core/src/test/java/.../integration/SelfTests.java` unmodified (copied verbatim from UC5) against the RBAC-modified `BankingToolService` produced the identical **14/14 pass** result UC5 recorded — this is the evidence that adding `AccessPolicy` and `StructuredAuditLogger` did not change any RAG-path behavior (policy answers, guardrail blocking, caching, citations all unaffected), only the live-data authorization path.

## New in UC6: RBAC and audit-logging test coverage

`rbac/RbacAndAuditSelfTests.java` — 14 hand-rolled tests, all passing:

| Test group | Count | What it proves |
|---|---|---|
| `AccessPolicy` (pure function) | 7 | The permission matrix in `docs/secure-backend-integration-design.md` is exactly what the code does — self-access, CUSTOMER-denied, SUPPORT_AGENT-elevated, ADMIN-elevated, unknown-role-denied, unknown-role-self-allowed, multi-role-any-qualifies |
| `BankingToolService` (real JWTs, real in-memory data) | 3 | SUPPORT_AGENT can read another customer's balance; that data is still masked (this test **caught a genuine assertion bug of my own** — see below); a plain CUSTOMER token is denied for another customer |
| `StructuredAuditLogger` | 4 | One JSON object per `log()` call; a raw account number never appears in the log even when the underlying data contains one; a forged token produces a `DENIED_AUTHENTICATION` record; an elevated access produces an `ALLOWED_ELEVATED` record naming the actual agent subject |

### A real bug this suite caught (in the test, not the system)

The first run of `testSupportAgentReceivedDataIsStillMasked` failed — the assertion checked for a literal `*` character in the masked account number, but `PiiMasking.maskAccountNumber` (UC3, unchanged) uses `X` as its mask character (`"XXXXXXXX0456"`, not `"********0456"`). This was a bug in the *test's* assumption, not in masking itself — confirmed by reading `PiiMasking.java`'s own Javadoc example inline. Fixed by correcting the assertion to check for `X`; rerun passed 14/14. Documented here rather than silently fixed, matching this submission's practice throughout (see UC4's threshold-labeling correction for the same disclosure standard).

## Frontend: what "evaluated" means for a UI, honestly

The React frontend has no accuracy/retrieval-quality dimension of its own — it renders whatever the backend returns. What was verified: `npm run build` and `npm run lint` both pass clean (`banking-support-frontend/reports/frontend-build-log.txt`), and a Playwright-driven browser session actually exercised both views against `mock-server/server.js` end-to-end, producing the screenshots in `banking-support-frontend/reports/frontend-screenshots/` — a policy answer with citations rendered correctly, and a banking dashboard rendering real (mocked) masked account data in a table. This is UI-contract verification, not RAG evaluation; see `banking-support-frontend/README.md` for the precise "real vs mock" boundary.
