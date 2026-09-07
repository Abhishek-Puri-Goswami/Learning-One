# L2 / UC5 — Final Integrated Banking RAG System

Gen-AI Use Case submission. Objective, per L2 HLD UseCase5: "Deliver a complete, integrated AI-powered banking support system demonstrating architectural maturity." This is the integration point for L2/UC1 (retrieval foundation), UC2 (RAG assistant), UC3 (secure banking data), and UC4 (observability) — one system, one entry point, every guardrail and verification from the prior four use cases intact.

## What's real vs. what's documented-but-unverified

Same split as every prior L2 use case, for the same reason (Maven Central blocked in this sandbox) — and this use case is where it matters most, because "integration" is exactly the kind of claim that's easy to assert and hard to verify without actually running it.

- **`banking-support-core/` was actually compiled and run**, wiring together — for the first time in one process — every previously-independently-verified component from UC1 through UC4: `SelfTests.java` (14 checks, hand-rolled) passed **14/14 on the first run**, each test exercising the FULL routed path (e.g., "a token for CUST1001 asking about CUST1002's balance is denied" exercises `IntentClassifier` → `BankingToolService` → `JwtService` → `UnauthorizedException`, not any one class in isolation). `Main.java`'s demo ran a realistic 9-turn mixed session — policy questions, all three live-data types, a cache hit, both guardrails, a cross-customer denial, and an ambiguous multi-intent query — in one continuous run. Every output in `reports/` is that run's actual, unedited output.
- **`banking-support-service/` (Spring Boot + Spring Security) could not be compile-verified** — same Maven Central block as every Spring Boot module in this submission. It composes the SAME already-verified classes from UC1-UC4 (copied as source, same pattern as every prior use case's service wrapper) behind one unified `POST /api/v1/support/ask` endpoint.

## Deliverables checklist (per L2 HLD UseCase5)

| Deliverable | Location | Status |
|---|---|---|
| Fully functional AI Banking Support System | `banking-support-core/` (compiled & run), `banking-support-service/` (Spring Boot wrapper) | Built & run |
| HLD and LLD documentation | `docs/hld-lld-documentation.md` | Built |
| Architecture diagrams | `diagrams/architecture-diagrams.md` (sequence + component diagrams, Mermaid) | Built |
| RAG evaluation summary | `evaluation/rag-evaluation-summary.md` | Built — synthesizes UC1/UC2/UC4's real evaluations plus this use case's own integrated confirmation |
| Performance & observability summary | `performance/performance-observability-summary.md` | Built — real integrated-session numbers, compared against UC4's isolated numbers |
| Final presentation | Delivered separately as a `.pptx` (see below) | Built |

## How to reproduce the real run

```bash
cd banking-support-core
mkdir -p out
javac -d out $(find src/main/java -name "*.java") $(find src/test/java -name "*.java")
java -cp out com.retailco.bankrag.integration.SelfTests    # expect: 14 passed, 0 failed
java -cp out com.retailco.bankrag.integration.Main corpus  # expect: same output as reports/integrated-demo-run-log.txt
```

## What this use case validated that no prior use case alone could

Every one of UC1-UC4 tested its own layer. This is the first point in the submission where "does routing actually send a policy question to RAG and a balance question to the secure banking tools, in the same process, using the real corpus and real JWTs" was tested and run for real — see `docs/hld-lld-documentation.md`'s closing section.

## Known limitations (disclosed, not hidden — inherited and unchanged from UC1-UC4)

1. **Maven Central blocked** — `banking-support-service/` not compile-verified here.
2. **No real LLM, no real LangSmith, no real embedding model** — same disclosed stand-ins as UC1/UC2, unchanged by integration.
3. **`ContextOptimizer` (UC4) still not wired into prompt-building** — a tracked, not silently dropped, gap.
4. **The 0.012 margin threshold is small-sample evidence-based, not final-production-validated** — see UC4's original caveat, still true here.
5. **No real login/credential flow** — `DevTokenController` (`@Profile("dev")`) is dev-only, same disclosed risk as UC3.

## Tech stack

Java 17, plain JDK for `banking-support-core` (by design), Spring Boot 3.3.4 + Spring Security for `banking-support-service` — consistent with this submission's confirmed Java Spring Boot + React stack across all five L2 use cases.
