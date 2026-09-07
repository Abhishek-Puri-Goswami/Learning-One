# L2 / UC3 — Secure Banking Data Integration

Gen-AI Use Case submission. Objective, per L2 HLD UseCase3: "Enable secure access to live banking data using controlled tool calling and authentication." This use case complements L2/UC2: UC2 answers policy questions from documents (guarded generation); UC3 answers live-data questions (account balance, transaction history, loan outstanding) from a real backend, with no LLM in the loop at all.

## What's real vs. what's documented-but-unverified

Same split as UC1 and UC2, and for the same underlying reason (Maven Central blocked in this sandbox) — but this use case went further than UC1/UC2 in one respect: **real cryptography**.

- **`secure-banking-core/` was actually compiled and run**, including a genuine from-scratch HS256 JWT implementation (`javax.crypto.Mac`, pure JDK — no `jjwt`/`io.jsonwebtoken` library needed). `SelfTests.java` (24 checks, hand-rolled) passed **24/24 on the first run**. `Main.java`'s demo ran 6 real scenarios: intent routing, a full happy-path token issuance + 3 data-tool calls, a real cross-customer authorization rejection, a **real forged-token rejection** (a token signed with a different secret, attempting to self-grant `ADMIN`, correctly fails cryptographic verification), a **real expired-token rejection** (based on the actual system clock), and a real ADMIN-role legitimate cross-customer access. Every output in `reports/` is that run's actual, unedited output.
- **Independently re-verified**: the demo's issued JWT was decoded outside this submission's own code, using Python's standard `base64`/`json` libraries, and confirmed to be a standards-shaped, correctly-formed JWT (`{"alg":"HS256","typ":"JWT"}` header, valid claims payload) — not just internally self-consistent.
- **`secure-banking-service/` (Spring Boot + Spring Security) could not be compile-verified** — same Maven Central block as every Spring Boot module in this submission. It reuses `secure-banking-core`'s already-verified `JwtService`/`BankingToolService`/`PiiMasking` classes unchanged rather than reimplementing the security-critical logic in an unverified form.

## Deliverables checklist (per L2 HLD UseCase3)

| Deliverable | Location | Status |
|---|---|---|
| Secure API endpoint | `secure-banking-service/.../controller/BankingDataController.java` (3 endpoints: balance, transactions, loans) | Written; logic behind it verified via `secure-banking-core` |
| JWT validation workflow | `secure-banking-core/.../security/JwtService.java` (crypto); `secure-banking-service/.../filter/JwtAuthenticationFilter.java` (Spring wiring) | Built & run (core); written (Spring wiring) |
| Tool-calling integration module | `BankingToolService.java`, `IntentClassifier.java`; `tool-calling/tool-calling-design.md` | Built & run |
| Structured response examples | `api-examples/structured-response-examples.md` | Real captured output |
| Security validation checklist | `security/security-validation-checklist.md` | 17/20 items verified, 1 disclosed residual risk, 1 configuration-discipline note, 1 (Spring Boot compile) blocked by sandbox limitation |

## How to reproduce the real run

```bash
cd secure-banking-core
mkdir -p out
javac -d out $(find src/main/java -name "*.java") $(find src/test/java -name "*.java")
java -cp out com.retailco.bankrag.security.SelfTests   # expect: 24 passed, 0 failed
java -cp out com.retailco.bankrag.security.Main        # expect: same output as reports/secure-banking-demo-run-log.txt
```

## Known limitations (disclosed, not hidden)

1. **Maven Central blocked** — `secure-banking-service/` (Spring Boot + Spring Security) not compile-verified here; run `mvn clean verify` with real internet access before deployment.
2. **No real login/credential flow** — `DevTokenController` issues a token for any requested `customerId` with no password/OTP/biometric check, and is explicitly marked dev-only (`@Profile("dev")`, `/dev/token`, a `warning` field in its own response). A real deployment authenticates the customer through a proper identity provider before this API's JWT-protected endpoints are ever reached — see `security/security-validation-checklist.md` item 17.
3. **Error-message text still differs between authentication and authorization failures**, even though the HTTP status/shape doesn't — a disclosed residual risk with a concrete fix recommended in `security/security-validation-checklist.md` item 14, not silently left out of the checklist.
4. **In-memory `BankingDataStore`, not a real relational database** — same local/dev-stand-in reasoning as L2/UC1's in-memory `VectorStore` standing in for pgvector; a real deployment would back this with the same PostgreSQL instance L1/L2 already use elsewhere, via Spring Data JPA, with the exact same method contracts.
5. **`IntentClassifier` is a keyword heuristic**, not a real LLM function-calling decision — disclosed in `tool-calling/tool-calling-design.md`, with the swap-in path to a real LLM-based tool selector once an LLM API is reachable.

## Tech stack

Java 17, plain JDK for `secure-banking-core` (by design, including hand-rolled HS256 JWT), Spring Boot 3.3.4 + Spring Security for `secure-banking-service` — consistent with this submission's confirmed Java Spring Boot + React stack.
