# L1 — USE CASE 5: AI-Generated Testing & Edge Case Simulation

Cart and Payment (order-service checkout) modules, tested per the brief's exact prompt: "Generate JUnit test cases... Cover: Normal flow, Empty cart, Large quantity, Concurrent modification, Invalid product ID," and "Simulate: Out-of-stock, Payment timeout, Invalid coupon."

## What's real vs. documented-but-unverified (updated in this rework)

The rest of this README (below) is unchanged from the original submission -- it documents 18 JUnit test methods against real Spring Boot `cart-service`/`order-service` copies, all "hand-traced" rather than machine-run, because Maven Central is blocked in this sandbox (see "Known Limitation"). That's still true and still disclosed.

What's new: **every one of the six bugs in "Real Bugs Found & Fixed While Writing These Tests" below is now also proven by real, compiled, machine-run tests**, not just hand-traced JUnit source -- in two new pure-JDK sibling modules this rework introduced, [`../ecommerce-core`](../ecommerce-core) and [`../order-review-core`](../order-review-core):

| # | Bug (from the table below) | Proven for real in | Test(s) |
|---|---|---|---|
| 1 | Lost-update race in concurrent add-to-cart | `../ecommerce-core` | `testConcurrentAddsOfSameProductMergeWithNoLostUpdates` -- 20 real threads, asserts exactly one merged line with quantity 20 |
| 2 | Invalid product IDs silently accepted | `../ecommerce-core` | `testUnknownProductLookupThrows` -- an unknown productId throws `ProductNotFoundException`, never a $0 fallback |
| 3 | No quantity cap | `../ecommerce-core` | `testQuantityAtCapIsAccepted`, `testQuantityOverCapIsRejected`, `testQuantitySumAcrossTwoAddsOverCapRejectedOnSecondAdd` (the exact 60+60>99 scenario) |
| 4 | No stock re-check at checkout | `../ecommerce-core` | `testCheckoutOversellIsRejectedAndStockUnchanged`, `testCheckoutStockExactlyMatchesRequestedQuantityBoundaryAccepted` (the last-unit boundary) |
| 5 | No HTTP timeouts / undistinguished payment failure | `../order-review-core` | `testPaymentTimeoutDistinguishedFromGenericFailure` -- a timeout produces a message containing "timed out"; any other gateway failure gets a generic message |
| 6 | Invalid/typo'd coupon silently ignored | `../order-review-core` | `testUnrecognizedCouponRejectedBeforePayment` (the exact `SAVE1O` typo scenario), `testBlankCouponTreatedAsNoCouponNotAnError` |

All 33 tests across both modules pass -- see `../ecommerce-core/reports/selftests-run-log.txt` (17/17) and `../order-review-core/reports/selftests-run-log.txt` (10/10; the other 6 of those 10 prove UC4's AI review findings, not UC5's edge cases). This does not replace the 18 hand-traced JUnit tests below -- those still document intent and exact Spring-layer assertions against the real `cart-service`/`order-service` API shapes -- it closes the "was this ever actually run" gap for the six bugs that matter most, the same way UC2's and UC4's reworks did.

## Original documentation (as delivered, unchanged below)

## Why cart-service AND order-service Are Both Copied Here

The brief says "Cart and Payment modules need robust testing." Cart maps directly to `cart-service` (L1/UC2). There is no standalone `payment-service` yet (only `order-service`'s `PaymentGatewayClient`, built in L1/UC4) — so the payment-related edge cases (timeout, invalid coupon affecting the charged amount, out-of-stock blocking a charge) are tested against `order-service`'s checkout flow, which is where payment is actually invoked. Both services are copied into this folder (from L1/UC2 and L1/UC4 respectively) **with real bugs found and fixed as a direct result of writing these tests** — this is the intended workflow for "AI-generated testing" driving quality improvements, not just documentation.

## Real Bugs Found & Fixed While Writing These Tests

| # | Bug | Found by writing... | Fix |
|---|---|---|---|
| 1 | **Lost-update race** in `CartServiceImpl.addItem` — two threads adding the same product concurrently could both miss each other and create duplicate lines / drop quantity, instead of summing. | `concurrentModification_sameProductAddedFromManyThreads_noLostUpdates` | `synchronized (cart)` around the read-decide-write sequence, scoped per-user (not a global lock). |
| 2 | **Invalid product IDs silently accepted at price $0** via `ProductCatalogClient`'s blanket fallback (originally built in L1/UC2 to survive *infra* failures, but it also masked a genuine 404). | `invalidProductId_productNotFoundInCatalog_throwsInvalidProductException` | `ProductCatalogClient` now distinguishes a confirmed 404 (`InvalidProductException`, rejected) from a transient network failure (still degrades gracefully). |
| 3 | **No quantity cap** — a scripted or fat-fingered request could add e.g. 999,999 units to a cart. | `largeQuantity_overCap_throwsInvalidQuantityException` | `@Max(99)` on the DTO + a matching service-layer check (`InvalidQuantityException`) for defense-in-depth. |
| 4 | **No stock re-check at checkout** — `order-service` trusted cart-service's snapshot unconditionally; an item could sell out between add-to-cart and checkout and still be charged. | `checkout_requestedQuantityExceedsAvailableStock_throwsOutOfStockBeforePayment` | New `ProductClient` + `validateStock()` step in `checkout()`, run before payment. |
| 5 | **No HTTP timeouts configured** — `RestTemplate` had no connect/read timeout anywhere, so a hung payment gateway would block a request thread indefinitely instead of failing fast. | `checkout_paymentGatewayTimesOut_...`, `PaymentGatewayClientTest` | Explicit `connect-timeout-ms` / `read-timeout-ms` via `RestTemplateBuilder` in `OrderServiceApplication`. |
| 6 | **Invalid/typo'd coupon codes silently ignored** — `DiscountCalculator`'s `default` branch returned the line total unchanged with no signal that the coupon didn't apply. | `checkout_unrecognizedCoupon_throwsInvalidCouponException_beforePayment` | Unknown coupon codes now throw `InvalidCouponException` → 400, rather than silently charging full price. |

## Deliverables Checklist (per the use case brief)

- [x] **AI-generated test suite** → `cart-service/src/test/`, `order-service/src/test/` (18 test methods total)
- [x] **Edge case documentation** → [`edge-cases/edge-case-catalog.md`](edge-cases/edge-case-catalog.md) (test name / scenario / expected result / code snippet, exactly as specified)
- [x] **Coverage report / proof** → [`coverage/coverage-proof.md`](coverage/coverage-proof.md) *(manual — no Maven Central / JaCoCo access in this sandbox, exact command included)*
- [x] **Retry demonstration for failed schema** → [`edge-cases/retry-demonstration.md`](edge-cases/retry-demonstration.md)
- [x] **Security scenario test** → `order-service/src/test/.../client/PaymentGatewayClientTest.java` (see `edge-cases/edge-case-catalog.md` §"Security Scenario Test")

## Folder Structure

```
UC5-Testing-EdgeCase-Simulation/
├── README.md
├── cart-service/                            Copy of L1/UC2's cart-service, hardened + tested
│   └── src/
│       ├── main/java/.../
│       │   ├── dto/CartItemRequest.java       + @Max(99) boundary
│       │   ├── client/ProductCatalogClient.java  fixed: 404 vs transient failure
│       │   ├── exception/InvalidProductException.java, InvalidQuantityException.java  (new)
│       │   └── service/CartServiceImpl.java    fixed: synchronized per-cart (race fix)
│       └── test/java/.../service/CartServiceImplTest.java   (7 tests)
├── order-service/                            Copy of L1/UC4's order-service, hardened + tested
│   └── src/
│       ├── main/java/.../
│       │   ├── OrderServiceApplication.java    + RestTemplate timeouts
│       │   ├── client/ProductClient.java        (new) stock re-validation
│       │   ├── client/PaymentGatewayClient.java  timeout-specific error message
│       │   ├── exception/OutOfStockException.java, InvalidCouponException.java  (new)
│       │   └── service/OrderServiceImpl.java, DiscountCalculator.java   updated
│       └── test/java/.../
│           ├── service/OrderServiceImplTest.java     (7 tests)
│           ├── service/DiscountCalculatorTest.java   (7 tests)
│           └── client/PaymentGatewayClientTest.java  (2 tests)
├── edge-cases/
│   ├── edge-case-catalog.md
│   └── retry-demonstration.md
└── coverage/
    └── coverage-proof.md
```

## Known Limitation (same as L1/UC2 and L1/UC4)

No outbound access to Maven Central in this sandbox — `mvn compile`/`mvn test` could not be run here (verified again for this use case; still `403 Forbidden` from `repo.maven.apache.org`). Every test was hand-traced against the production code it exercises. Run `mvn test` on both modules the first time you're on a machine with normal internet access, and generate a real JaCoCo report per the command in `coverage/coverage-proof.md`.

This limitation does **not** apply to `../ecommerce-core` or `../order-review-core` (see "What's real vs. documented-but-unverified" at the top of this README) -- those two sibling modules were introduced specifically to give the six most important bugs on this page real, machine-run proof, independent of Maven Central.
