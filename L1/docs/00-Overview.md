# L1 — Gen-AI Use Case Submission (5 independent use cases)

This repo holds L1's five independent use cases as one cumulative Java Full Stack project, built and committed incrementally: one feature branch per use case, merged into `main` after each, so `main`'s history shows the project growing use case by use case rather than arriving as one final drop.

L1's use cases are independent exercises (not a single continuously-scoped product the way L2 and L3 are) -- but UC2, UC4, and UC5 all touch the same e-commerce domain (product catalog, cart, checkout), so this rework introduced two shared, pure-JDK modules those three use cases build on:

- **`ecommerce-core`** -- product/cart/checkout domain logic, introduced in UC2, extended in UC5 (quantity cap, concurrency safety).
- **`order-review-core`** -- the before/after code-review proof module, introduced in UC4, extended in UC5 (coupon validation, payment-timeout distinction).

Both compile and run with plain `javac`/`java` -- no Maven Central needed -- and both are real, tested, evidence-backed modules, not documentation. See each module's own README for what it proves and how.

## Use cases

| # | Use case | Branch (merged) | Depends on |
|---|---|---|---|
| 1 | Requirements & Architecture Design | `uc1-requirements-architecture` | -- (docs only, per the use case's own spec) |
| 2 | Backend API Scaffolding (Contract-First) | `uc2-backend-api-scaffolding` | `ecommerce-core` (introduced here) |
| 3 | Frontend UI Generation & Risk Control | `uc3-frontend-ui-generation` | -- (a real, built-and-tested Vite/React app, independent of the Java core) |
| 4 | AI-Assisted Code Review & Quality Governance | `uc4-code-review-quality-governance` | `order-review-core` (introduced here) |
| 5 | AI-Generated Testing & Edge Case Simulation | `uc5-testing-edgecase-simulation` | `ecommerce-core`, `order-review-core` (both extended here) |

Run `git log --all --graph --oneline` to see the full history: five feature branches, each merged into `main` in order, `main` accumulating one use case's real, working code at a time.

## What's real vs. documented-but-unverified, across all five

Maven Central is blocked in both this project's original build sandbox and on the machine this repo is delivered to (`403 Forbidden, X-Proxy-Error: blocked-by-allowlist` against `repo.maven.apache.org`, confirmed independently in both places). Every Spring Boot module in this repo is therefore **documented, not compile-verified** -- written and hand-reviewed against its OpenAPI contract / Sonar rule catalog / test-source intent, but never run through `mvn compile`.

What changed in this rework is that the actual business logic those Spring modules depend on -- product/cart/checkout rules, the AI-review-finding bugs and their fixes, the UC5 edge cases with real behavior to test -- now lives in `ecommerce-core` and `order-review-core`, two modules with **zero external dependencies**, compiled and run for real with hand-rolled test suites (27 tests total, all passing; see each module's `reports/`). npm/Vite tooling (used by UC3's frontend) is not blocked the way Maven Central is, so that use case was always genuinely built and tested, unaffected by this rework.

See each use case's own README for its specific real/unverified breakdown.
