# L2 / UC6 — Final Integrated Banking RAG System (with CI/CD)

Gen-AI use case submission. Objective, per L2 HLD USECASE 6: "Deliver a fully integrated, production-ready Banking RAG System that supports secure data retrieval, grounded responses, observability, compliance, and automated CI/CD deployment." Builds directly on `L2/UC5-Final-Integrated-System/` — the same `IntegratedBankingAssistant` routing, the same verified UC1-UC4 components — and adds this use case's three functional-scope additions: RBAC, structured/audit logging, and an operational CI/CD pipeline, plus the frontend and containerization deliverables neither UC5 nor any earlier L2 use case built.

## What's new here versus UC5 (not a repeat)

| Addition | Where |
|---|---|
| RBAC (`CUSTOMER`/`SUPPORT_AGENT`/`ADMIN`, a named permission matrix, two enforcement layers) | `banking-support-core/.../security/Role.java`, `AccessPolicy.java`; `banking-support-service/.../controller/AuditController.java` |
| Structured audit logging of every access decision, PII-safe by construction | `banking-support-core/.../logging/StructuredAuditLogger.java` |
| Structured HTTP access logging (service module) | `banking-support-service/.../logging/HttpAccessLogFilter.java` |
| React frontend — policy Q&A UI + banking dashboard | `banking-support-frontend/` (built AND run end-to-end, see below) |
| CI/CD pipeline (lint, test, evaluate, security-scan, build, deploy-gated) | `.github/workflows/ci-cd.yml`, `cicd/README.md` |
| Containerization (Dockerfiles, docker-compose, k8s manifests) | `banking-support-service/Dockerfile`, `banking-support-frontend/Dockerfile`, `deployment/` |

## What's real vs. what's documented-but-unverified — the honest split, by component

| Component | Status |
|---|---|
| `banking-support-core/` (Java, pure JDK) | **Actually compiled and run.** `integration.SelfTests` 14/14, `rbac.RbacAndAuditSelfTests` 14/14, full 10-turn demo (adds a SUPPORT_AGENT RBAC scenario to UC5's 9 turns) — all real output in `banking-support-core/reports/` |
| `banking-support-frontend/` (React + Vite) | **Actually built, linted, and run end-to-end.** `npm run build`/`npm run lint` both pass clean; a Playwright-driven browser session exercised both views against a contract-matching mock backend — screenshots in `banking-support-frontend/reports/frontend-screenshots/` |
| `banking-support-service/` (Spring Boot) | **`mvn compile`-verified** on a machine with real Maven Central access |
| OpenAI integration (`OpenAiEmbeddingModel`, `OpenAiLlmClient`) | **Implemented and build-verified** — real `/v1/embeddings` and `/v1/chat/completions` calls over pure-JDK `HttpClient`, used automatically when `OPENAI_API_KEY` is set (`isConfigured()` switch in `Main.java` and `IntegratedAssistantConfig`), falling back to `LocalHashingEmbeddingModel`/`ExtractiveStubLlmClient` otherwise. A live call against a real OpenAI-compatible endpoint was confirmed end-to-end in the sibling UC1/UC2 modules, which share this exact same client code; this module's own live path was not independently re-run |
| `.github/workflows/ci-cd.yml` | **5 of 8 jobs' underlying commands were actually run directly** in this sandbox (lint, core-tests, rbac-and-security-tests, evaluation, frontend-build-and-lint); the other 3 (Maven build, Docker build, deploy) need infrastructure this sandbox doesn't have — see `cicd/README.md`'s table |
| `deployment/docker-compose.yml`, `deployment/k8s/*.yaml` | **Syntax/schema-validated**, not build/run-verified (no Docker daemon, no k8s cluster here) |

## Deliverables checklist (per L2 HLD UseCase6 + LLD sections 6-8)

| Deliverable | Location | Status |
|---|---|---|
| Fully functional AI Banking RAG System | `banking-support-core/` (compiled & run), `banking-support-service/` (Spring Boot wrapper) | Built & run (core) / Built (service) |
| Architecture documentation & diagrams | `diagrams/architecture-diagrams.md` | Built |
| Secure backend integration design | `docs/secure-backend-integration-design.md` | Built |
| Evaluation summary | `evaluation/evaluation-summary.md` | Built |
| Performance & PII-masking report | `performance/performance-pii-masking-report.md` | Built — real run numbers |
| Operational CI/CD pipeline | `.github/workflows/ci-cd.yml`, `cicd/README.md` | Built — 5/8 jobs' commands actually run here |
| Frontend Interface (policy query UI + banking dashboard) | `banking-support-frontend/` | Built & run end-to-end (against mock backend) |
| CI/CD assets | `.github/workflows/ci-cd.yml` | Built |
| Automated test suite (RAG accuracy, banking API, masking security, unauthorized access) | `banking-support-core/src/test/java/.../integration/SelfTests.java` + `.../rbac/RbacAndAuditSelfTests.java` | Built & run — 28/28 passing across both suites |
| Deployment model (local + containerization) | `deployment/deployment-model.md`, `deployment/docker-compose.yml`, `deployment/k8s/` | Built |

## How to reproduce the real runs

```bash
cd banking-support-core
mkdir -p out
javac -d out $(find src/main/java -name "*.java") $(find src/test/java -name "*.java")
java -cp out com.retailco.bankrag.integration.SelfTests       # expect: 14 passed, 0 failed
java -cp out com.retailco.bankrag.rbac.RbacAndAuditSelfTests  # expect: 14 passed, 0 failed
java -cp out com.retailco.bankrag.integration.Main corpus     # expect: output matching banking-support-core/reports/integrated-demo-run-log.txt (latency figures will vary slightly)

cd ../banking-support-frontend
npm install
npm run lint    # expect: 0 issues
npm run build   # expect: clean build, ~200KB JS / ~62KB gzip
node mock-server/server.js &   # in a second terminal
npm run dev                    # http://localhost:5173
```

## Known limitations (disclosed, not hidden — several inherited from UC1-UC5, unchanged)

1. **`mvn compile` now verified** for `banking-support-service/` on a machine with real Maven Central access; the `docker-build` CI job's commands are still not literally executed in this sandbox.
2. **No real LangSmith** — same disclosed stand-in as UC1/UC2. Real OpenAI embedding/LLM integration is now implemented (see table above); `LocalHashingEmbeddingModel`/`ExtractiveStubLlmClient` remain the automatic offline fallback when no API key is set.
3. **`ContextOptimizer` (UC4) still not wired into prompt-building** — a tracked, not silently dropped, gap.
4. **The 0.012 margin threshold is small-sample evidence-based** — see UC4's original caveat, still true here.
5. **No real login/credential flow** — `DevTokenController` (`@Profile("dev")`) is dev-only, same disclosed risk as UC3/UC5.
6. **`StructuredAuditLogger` writes synchronously** — adds unmeasured latency to every live-data call; a production system would want this async/batched (see `performance/performance-pii-masking-report.md`).
7. **Frontend has never talked to the real backend** — only to the contract-matching mock server; see `banking-support-frontend/README.md`'s honest boundary.
8. **Containerization and CI/CD's build/deploy stages are unexecuted in this sandbox** — Dockerfiles and the k8s manifests are validated for syntax, not for a real build/apply.

## Tech stack

Java 17, plain JDK for `banking-support-core` (by design, so it can be actually compiled and run here), Spring Boot 3.3.4 + Spring Security for `banking-support-service`, React 19 + Vite for `banking-support-frontend` — consistent with this submission's confirmed Java Spring Boot + React stack across every L2 use case.
