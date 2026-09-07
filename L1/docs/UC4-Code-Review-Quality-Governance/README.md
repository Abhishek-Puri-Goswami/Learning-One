# L1 — USE CASE 4: AI-Assisted Code Review & Quality Governance

Order Service, reviewed before deployment: AI review vs. a Sonar-rule-catalog-based static review, then refactored to fix every finding.

## Why order-service Exists Here (not just UC2)

`order-service` (order-management-service in L1/UC1's architecture) was not yet scaffolded in L1/UC2 — that use case only covered Product and Cart. Since UC4 requires reviewing "the Order Service code," it's built here in two deliberate versions: a **before** version seeded with the exact defect classes the brief calls out, and an **after** version with every finding fixed. Both plug into the same contracts as UC2/UC3 (calls `cart-service` on `:8082`, will itself run on `:8083` as referenced in UC3's `vite.config.js` proxy).

## Deliverables Checklist (per the use case brief)

- [x] **AI review report** → [`reviews/ai-review-report.json`](reviews/ai-review-report.json) (matches the exact schema from the use case's example prompt: `security_issues`, `complexity_score`, `refactor_suggestions`, `risk_level`, plus `hallucinated_apis` and `licensing_concerns` per the brief's checklist)
- [x] **SonarQube report** → [`reviews/sonarqube-style-report.md`](reviews/sonarqube-style-report.md) *(manual, rule-catalog-based — see the limitation note inside; no SonarQube/Maven Central access in this sandbox)*
- [x] **Refactored code** → [`order-service-refactored/`](order-service-refactored/)
- [x] **Quality improvement summary** (incl. "document the differences") → [`quality-improvement-summary.md`](quality-improvement-summary.md)

## Folder Structure

```
UC4-Code-Review-Quality-Governance/
├── README.md
├── quality-improvement-summary.md          AI vs Sonar-style comparison + before/after metrics
├── order-service-before/                   Input artifact -- intentionally flawed, DO NOT deploy
│   ├── pom.xml                              (includes an unused AGPL dependency -- license finding)
│   └── src/main/java/.../
│       ├── client/PaymentGatewayClient.java  Hard-coded API key + swallowed exception + always-true bug
│       ├── client/CartClient.java            Calls a hallucinated /checkout-summary endpoint
│       ├── service/OrderServiceImpl.java     Missing null check + cyclomatic complexity 19
│       └── controller/OrderController.java   No @Valid, unused import
├── order-service-refactored/               Output artifact -- every finding fixed
│   ├── pom.xml                              (AGPL dependency removed)
│   ├── .env.example                         How the payment API key is externalized
│   └── src/
│       ├── main/java/.../
│       │   ├── client/PaymentGatewayClient.java   Key from config; throws PaymentFailedException on failure
│       │   ├── client/CartClient.java             Calls the REAL cart-service endpoint from L1/UC2's contract
│       │   ├── service/OrderServiceImpl.java      Decomposed into 5 small methods, complexity ~4
│       │   ├── service/DiscountCalculator.java    Extracted coupon logic
│       │   ├── exception/                         PaymentFailedException, EmptyCartException, GlobalExceptionHandler
│       │   └── controller/OrderController.java     @Valid added, unused import removed
│       └── test/java/.../service/
│           ├── OrderServiceImplTest.java          Happy path, empty cart, payment failure, discount application
│           └── DiscountCalculatorTest.java         Every coupon/payment-method/city branch
└── reviews/
    ├── ai-review-report.json
    └── sonarqube-style-report.md
```

## What's real vs. documented-but-unverified (updated in this rework)

No outbound access to Maven Central or a SonarQube server in this sandbox, so neither Spring module was machine-compiled or machine-scanned here. Every finding was verified by hand against the actual OpenAPI contracts from L1/UC2 and against the real Sonar Java rule catalog (rule keys are genuine, not invented). Run `mvn -q compile` and a real `sonar-maven-plugin` scan on both modules the first time you're on a machine with normal internet access — see `reviews/sonarqube-style-report.md` for the exact command.

That was the state of this use case before this repo's rework. It's no longer the whole story: **[`../order-review-core`](../order-review-core)** — a new, pure-JDK sibling module this rework introduced — reproduces every finding above that has actual runtime behavior (the discount-logic complexity refactor, AI-SEC-2's swallowed-payment-failure bug, AI-QA-1's null-shippingAddress NPE) as plain Java, compiles it for real, and runs a hand-rolled test suite (6/6 passing, see `../order-review-core/reports/`) that proves each "before" bug is genuinely reproducible and each "after" fix genuinely resolves it — including a test that the refactor's discount arithmetic is behavior-identical to the original nested-if logic across 9 branch combinations, not just structurally cleaner. See `../order-review-core/README.md` for the finding-by-finding mapping. The two findings with no runtime behavior to test (the hard-coded API key literal, the hallucinated endpoint URL) are still verified by direct source inspection, as before — that remains the right kind of evidence for a fact rather than a behavior.
