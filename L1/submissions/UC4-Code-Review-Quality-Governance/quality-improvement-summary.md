# Quality Improvement Summary — L1 UC4

Deliverable: "Quality improvement summary" + "Document differences" (AI review vs SonarQube-style review). Covers `order-service` before (`order-service-before/`) and after (`order-service-refactored/`) this code review pass.

## 1. Findings Overlap: AI Review vs. Sonar-Style Review

| Finding | AI Review (`ai-review-report.json`) | Sonar-Style Review (`sonarqube-style-report.md`) | Who found it first / better |
|---|---|---|---|
| Hard-coded payment API key | ✅ AI-SEC-1, CRITICAL | ✅ #1, Blocker, `squid:S2068` | **Tie** — this is a well-known pattern-matchable rule; both catch it reliably. |
| Payment failure swallowed & reported as success | ✅ AI-SEC-2, CRITICAL, explains the *financial* consequence | ✅ #2/#3, Critical/Major, `squid:S1166`/`squid:S3516`, but as two separate syntactic rule hits | **AI review wins on explanation** — it connects the two rule-level smells into one business-level story ("a failed payment becomes a confirmed order"), which a raw Sonar issue list does not do on its own. |
| Missing null check on shippingAddress | ✅ AI-QA-1, HIGH | ✅ #4, Critical, `squid:S2259` | **Tie**, though Sonar's static dataflow analysis is what reliably proves the null-dereference path; the AI's reasoning was contextual ("if the field is omitted from the request"), which happens to be correct here but is not a formal proof the way Sonar's engine provides. |
| High complexity in `checkout()` | ✅ AI-QA (complexity_score object), estimated CC 19 / CogC 24 | ✅ #5/#6, Critical/Major, `squid:S3776`/`squid:S138` | **Sonar wins on precision** — Sonar computes complexity deterministically from the AST; the AI's numbers are estimates (and were labeled as such) that happened to land close to a plausible real measurement. |
| Hallucinated `/checkout-summary` endpoint | ✅ AI-API-1, CRITICAL — **only found by comparing against the actual OpenAPI contract** | ❌ Not found — a generic Sonar Java ruleset has no concept of "this endpoint doesn't exist on the real service," since that's a cross-service contract issue, not a syntax/AST-level Java defect | **AI review wins outright** — this is exactly the class of bug static analysis tools structurally cannot catch, and exactly why "avoid hallucinated APIs" was called out as its own required checklist item in the use case brief. |
| Unused import | ✅ AI-QA-3, LOW | ✅ #7, Minor, `squid:S1128` | **Tie**, trivial for both. |
| Missing request validation (`@Valid`) | ✅ AI-QA-2, MEDIUM | ✅ #8, Major (mapped to an analogous validation rule) | **Tie**, though the AI's reasoning ("inconsistent with product-service/cart-service") is a cross-file consistency observation Sonar's per-file rule engine doesn't naturally produce. |
| AGPL-licensed dependency (unused) | ✅ AI-LIC-1, HIGH, with a concrete remediation (remove or swap for PDFBox) | ✅ #9, Major, via license-check integration (not a core Java rule) | **Tie in detection, AI wins on remediation specificity** — Sonar's license-check plugins typically just flag the SPDX license; the AI review additionally noticed the dependency was *unused* and recommended outright removal rather than a commercial license purchase. |
| Zero test coverage | ✅ AI-QA-4, HIGH | ✅ #10, Info/Coverage gap | **Tie** in detection; addressed identically (both flag it as a gap to close). |

## 2. Conclusion on AI vs. Sonar

- **Sonar-style/static analysis is more reliable for**: deterministic, syntactic/AST-provable issues (unused imports, exact complexity metrics, provable null-dereference paths) — because it's built on a real parser and dataflow engine, not inference.
- **AI review is more valuable for**: cross-artifact contextual issues that require understanding *intent* and *other documents* — the hallucinated-endpoint finding (AI-API-1) required comparing this code against a separate OpenAPI file from a different use case folder (L1/UC2), something a single-file static analyzer has no way to do. AI review also tends to narrate the *business impact* of a finding (e.g., "a failed payment becomes a confirmed order") rather than just the rule violation.
- **Practical takeaway**: use both. Static analysis (Sonar or equivalent) should gate CI automatically on the deterministic issues; AI review is a complementary step best used for cross-contract/cross-document consistency checks and for producing the narrative "why this matters" that helps a human reviewer prioritize.

## 3. Before → After Metrics

| Metric | Before (`order-service-before`) | After (`order-service-refactored`) |
|---|---|---|
| Critical/Blocker security findings | 3 (hard-coded key, swallowed payment failure, hallucinated endpoint) | 0 |
| `checkout()` cyclomatic complexity (estimated) | 19 | ~4 |
| `checkout()` cognitive complexity (estimated) | 24 | ~5 |
| Methods in checkout flow | 1 (`checkout()` does everything) | 5 (`checkout`, `fetchNonEmptyCart`, `priceLines`, `buildConfirmedOrder`, `toResponse`) + `DiscountCalculator` extracted to its own class |
| Request validation | None | `@Valid` + Bean Validation on `OrderRequest` and nested `ShippingAddressDto` |
| Hard-coded secrets | 1 (payment API key) | 0 (externalized via `${PAYMENT_GATEWAY_API_KEY}`, see `.env.example`) |
| Unapproved copyleft dependencies | 1 (iTextPDF, AGPL-3.0, unused) | 0 (removed) |
| Unit test files | 0 | 2 (`OrderServiceImplTest`, `DiscountCalculatorTest`) — 4 + 7 test cases covering happy path, empty cart, payment failure, and every discount branch |
| Quality gate (simulated "Sonar way") | **FAILED** (4 new Critical/Blocker issues, 0% coverage) | **Would PASS** *(pending an actual Sonar run — see limitation note below)* — 0 known Critical/Blocker issues remain, meaningful test coverage added on the previously-untested `checkout()`/`DiscountCalculator` logic |

## 4. Outstanding Limitation

As documented in `order-service-before/pom.xml`, `order-service-refactored/pom.xml`, and `reviews/sonarqube-style-report.md`: this sandbox cannot reach Maven Central or a SonarQube server, so neither version was compiled or scanned by real tooling here. Both `mvn compile` and an actual `sonar-maven-plugin` run should be executed on a machine with normal network access as the final verification step before this is considered "review-complete" in a real delivery — the same caveat noted in L1/UC2.
