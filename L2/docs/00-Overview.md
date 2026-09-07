# L2 — Banking RAG Assistant (6 cumulative use cases)

Unlike L1 (five independent exercises), L2 is one continuously-scoped project: a Retrieval-Augmented-Generation assistant for retail-banking policy questions, built incrementally across six use cases, each extending the previous one's working system rather than starting over. This repo captures that as real git history: one branch per use case, merged into `main` after each, so `main`'s commit history shows the same incremental build-up the use cases themselves describe.

## Use cases (cumulative)

| # | Use case | Branch (merged) | Adds |
|---|---|---|---|
| 1 | Foundation & Core Retrieval | `uc1-foundation-core-retrieval` | `rag-core` (pure-JDK retrieval engine), `rag-service` (Spring Boot wrapper, documented-unverified) |
| 2 | End-to-End RAG Assistant | `uc2-endtoend-rag-assistant` | LLM-answer synthesis, citations, a real React frontend |
| 3 | Secure Banking Data Integration | `uc3-secure-banking-data-integration` | JWT auth, PII masking, live banking-data tools alongside the RAG path |
| 4 | Intelligence Maturity & Optimization | `uc4-intelligence-maturity-optimization` | Caching, cost/latency metrics, guardrail evaluation, observability tracing |
| 5 | Final Integrated System | `uc5-final-integrated-system` | All of the above wired into one coherent assistant; full evaluation summary |
| 6 | Final Integrated System with CI/CD | `uc6-final-integrated-system-cicd` | RBAC, structured audit logging, a GitHub Actions CI/CD pipeline, Docker/K8s deployment artifacts |

Run `git log --all --graph --oneline` to see the full history: six feature branches, each merged into `main` in order, `main` accumulating one use case's real, working code at a time.

## What's real vs. documented-but-unverified

Maven Central is blocked in both this project's original build sandbox and on the machine this repo is delivered to (`403 Forbidden, X-Proxy-Error: blocked-by-allowlist` against `repo.maven.apache.org`). Every use case's core retrieval/business logic (`rag-core` and its extensions) is **pure JDK, zero external dependencies, compiled and run for real** at every stage of this history -- the Spring Boot wrapper (`rag-service`) stays documented-but-not-compile-verified throughout, consistently disclosed in each use case's own README. UC2 onward also includes a real React/Vite frontend (UC6 in particular), genuinely built, linted, and browser-tested with Playwright -- npm's registry is reachable in this sandbox even though Maven Central isn't.

See each use case's own README for its specific real/unverified breakdown and reports.
