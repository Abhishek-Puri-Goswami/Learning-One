# order-review-core

Pure-JDK proof module shared by L1/UC4 (AI-Assisted Code Review & Quality Governance) and L1/UC5 (Testing & Edge Case Simulation). It does not replace `../UC4-Code-Review-Quality-Governance/order-service-before`/`order-service-refactored` or `../UC5-Testing-EdgeCase-Simulation/order-service` -- those remain the actual deliverables (the real Spring Boot services, the AI review report, the edge-case catalog). This module exists to answer the question those Spring services alone couldn't, because Maven Central is blocked in this sandbox: **are these findings and edge cases real, and do the fixes actually work, without changing the behavior that's supposed to stay the same?**

## What this proves, and how

### UC4: AI review findings (6/10 tests)

Every code-level finding in `../UC4-Code-Review-Quality-Governance/reviews/ai-review-report.json` that has a "before" and "after" code shape is reproduced here as plain Java (no Spring, no HTTP client -- just the control flow the review is actually about):

| Finding | Before (bug reproduced) | After (fix confirmed) | Test |
|---|---|---|---|
| Discount logic refactor (complexity finding, `OrderServiceImpl.checkout()`, cyclomatic complexity 19) | `before.BeforeDiscountPricer` -- near-verbatim port of the original nested if/else | `after.AfterDiscountCalculator` -- pure-JDK port of the extracted, switch-expression-based class | `testRefactorPreservesDiscountBehaviorAcrossAllBranches` -- runs 9 coupon/payment-method/city/threshold combinations through both and asserts identical totals. This is the load-bearing test: it's not enough that the refactor *looks* cleaner, this proves it computes the same numbers. |
| AI-SEC-2 (payment failures swallowed, reported as success) | `before.BeforePaymentGateway.charge()` -- reproduces the exact empty-catch-then-return-true bug | `after.AfterPaymentGateway.charge()` -- throws `PaymentFailedException` on any gateway failure | `testBeforePaymentBugReportsSuccessOnFailure` (proves the bug is real -- a failing call really does report success), `testAfterPaymentFixPropagatesFailure`, `testAfterPaymentSucceedsSilentlyOnRealSuccess` |
| AI-QA-1 (`shippingAddress.getCity()` NPE) | `before.BeforeCheckoutFlow.resolveCity(null)` | `after.AfterCheckoutFlow.resolveCity(null)` -- fails with a named `IllegalArgumentException` instead of an opaque NPE (mirrors the real fix, which relies on `@Valid @NotNull` rejecting the request before the service layer runs at all) | `testBeforeNullShippingAddressThrowsUnguardedNpe`, `testAfterNullShippingAddressFailsWithClearError` |

Findings NOT reproduced here (and why that's fine): AI-SEC-1 (hard-coded API key) and AI-API-1 (hallucinated cart-service endpoint) are structural/textual facts checkable by reading the source directly (the key literal is either present or absent; the URL either matches the OpenAPI contract or doesn't) -- there's no behavior to run a test against, so `reviews/sonarqube-style-report.md`'s line-number citation is the right form of evidence for those two, not a test.

### UC5: payment/coupon edge cases (4/10 tests)

`../UC5-Testing-EdgeCase-Simulation/edge-cases/edge-case-catalog.md` documents two payment/checkout edge cases beyond what UC4's refactor covered. This module closes that gap for real rather than leaving it as "hand-traced":

| Edge case (from UC5's catalog) | What's new here | Test |
|---|---|---|
| Invalid coupon (`checkout_unrecognizedCoupon_throwsInvalidCouponException_beforePayment`, `DiscountCalculatorTest.unknownCoupon_...`) | `after.CouponPolicy.validate()` -- rejects an unrecognized code (e.g. the typo `SAVE1O`) with `InvalidCouponException`; a blank/absent code is correctly treated as "no coupon," not an error. This is genuinely new logic, not present in UC4's `AfterDiscountCalculator` (which silently no-ops on any unrecognized string) -- UC5's catalog is right that a typo'd coupon should fail loudly, so this module adds the validator UC4's version was missing. | `testUnrecognizedCouponRejectedBeforePayment`, `testBlankCouponTreatedAsNoCouponNotAnError`, `testRecognizedCouponPassesValidation` |
| Payment timeout distinguished from a generic gateway failure (`charge_gatewayTimesOut_...` vs `charge_gatewayReturns5xx_...` in `PaymentGatewayClientTest`) | `after.AfterPaymentGateway.charge()` now inspects the failure cause: a `TimeoutException` (this module's stand-in for a wrapped `SocketTimeoutException`) produces a message containing "timed out"; any other failure gets a generic message. | `testPaymentTimeoutDistinguishedFromGenericFailure` |

Edge cases NOT reproduced here: the cart-side edge cases (quantity cap, concurrent same-product adds, invalid productId) are covered instead in `../ecommerce-core` -- see that module's README and `reports/selftests-run-log.txt` (17/17, including `testQuantityAtCapIsAccepted`, `testConcurrentAddsOfSameProductMergeWithNoLostUpdates`, and the checkout stock-boundary test) -- since those are cart/catalog concerns, not order/payment ones, and `ecommerce-core` is where that domain already lives.

## What's real vs. documented-but-unverified

| Layer | Status |
|---|---|
| `order-review-core` (this module) | **Real** -- compiles and runs with plain `javac`/`java`, 10/10 self-tests passing; see `reports/` |
| `order-service-before`, `order-service-refactored` (UC4's Spring Boot deliverables) | Documented, not compile-verified -- Maven Central blocked, same as every other Spring module in this submission |
| `order-service` (UC5's Spring Boot deliverable, with its own JUnit/Mockito test classes) | Documented, not compile-verified -- same limitation; its test *source* is real and readable, but was never machine-run, which is exactly the gap this module's payment/coupon tests close for the two edge cases that matter most (a failed-payment-reported-as-success bug and a silently-discounted typo) |
| `reviews/ai-review-report.json`, `reviews/sonarqube-style-report.md`, `edge-cases/edge-case-catalog.md` | Documents, as originally delivered -- now backed by real test evidence (here and in `ecommerce-core`) for every finding/case that has runnable behavior |

## Reproducing the verified run locally

```bash
cd order-review-core
javac -Xlint:all -d out $(find src/main/java -name "*.java") $(find src/test/java -name "*.java")
java -cp out com.retailco.orderreview.selftest.SelfTests   # 10/10, see reports/selftests-run-log.txt
```
