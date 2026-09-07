# ecommerce-core

Shared, pure-JDK domain module for L1's e-commerce use cases (UC2 Backend API
Scaffolding, UC4 Code Review & Quality Governance, UC5 Testing & Edge-Case
Simulation). It exists so those three use cases share one real, compiled,
tested product/cart/checkout implementation instead of each re-describing
the same domain in documentation only.

## Why this module exists (the gap it closes)

L1's original UC2/UC4/UC5 deliverables contained real Java source (`Product`,
`Cart`, `CartItem`, controllers, refactor pairs, test classes) but none of it
was ever machine-compiled or run -- Maven Central is blocked in this
environment (confirmed both in the build sandbox and on the user's own
device: `403 Forbidden, X-Proxy-Error: blocked-by-allowlist` against
`repo.maven.apache.org`), and the Spring Boot dependencies those modules
declared made that a hard blocker for `javac` too.

This module applies the same fix L2 and L3 already use: implement the real
business logic (domain model + stock reservation + checkout + a stubbed
payment gateway) as **plain Java with zero external dependencies**, so it
compiles and runs with nothing but the JDK already on this machine. The
Spring Boot **wrapper** around it (REST controllers, `@RestController`,
JPA repositories) still needs Maven Central and stays documented-but-not-
compile-verified, exactly like every other Spring module in this submission
-- but the actual business rules underneath it are no longer just claimed,
they're proven.

## What's real vs. documented-but-unverified

| Layer | Status |
|---|---|
| `model/` (Product, Cart, CartItem, Order, OrderLine, OrderStatus) | **Real** -- compiles, runs |
| `catalog/ProductCatalog` (stock reservation, oversell prevention) | **Real** -- compiles, runs, concurrency-tested |
| `cart/CartService` | **Real** -- compiles, runs |
| `order/CheckoutService` (reserve → charge → commit/rollback) | **Real** -- compiles, runs |
| `payment/StubPaymentGateway` | **Real, explicitly a stub** -- deterministic, no network call (a real gateway needs external network access this sandbox doesn't have) |
| `pom.xml` | Documented, not compile-verified (needs Maven Central) |
| L1/UC2's Spring Boot `product-service`/`cart-service` wrappers | Documented, not compile-verified -- see that use case's own README for how it maps onto this core |

## Field/model fidelity to the original L1/UC2 source

`Product`, `Cart`, and `CartItem` in this module intentionally reuse the
exact field names and types already present in L1/UC2's original
(uncompiled) `product-service`/`cart-service` source -- `Product{id, name,
description, price, category, stockQuantity, createdAt, updatedAt}`,
`Cart{userId, items, updatedAt}`, `CartItem{itemId, productId, productName,
unitPrice, quantity}` plus its `getLineTotal()` calculation -- so the
Spring Boot wrapper layer can sit on top of this verified core without a
field-by-field remap.

## Domain rules this module actually enforces (and proves, not just asserts)

- **Never oversell.** `ProductCatalog.reserveStock` is the single choke
  point every stock decrement goes through; it's `synchronized` and throws
  `InsufficientStockException` rather than clamping. Proven under real
  concurrent load, not just sequentially -- see
  `testConcurrentCheckoutsCannotOversellLastUnit` (8 threads race for the
  last unit of a product; exactly one wins, stock ends at exactly 0).
- **All-or-nothing checkout.** A multi-line cart where an early line
  reserves successfully but a later line fails rolls the earlier
  reservation back -- proven by
  `testCheckoutPartialReservationRollsBackOnLaterLineFailure`.
- **A declined payment releases its reservation.** Stock is reserved
  before charging (so two buyers can't both be told "success" for the
  same last unit), but a decline afterward gives that stock back --
  proven by `testCheckoutDeclinedPaymentReleasesReservedStock`.
- **A per-line quantity cap that applies to the running total, not just one
  request.** `CartService.MAX_QUANTITY_PER_LINE` (99) rejects a single
  over-cap add and a second add that would push a line's cumulative
  quantity over the cap -- proven by `testQuantityAtCapIsAccepted`,
  `testQuantityOverCapIsRejected`, and
  `testQuantitySumAcrossTwoAddsOverCapRejectedOnSecondAdd` (this last one
  is L1/UC5's edge-case catalog requirement verbatim: 60 + 60 of the same
  product must reject on the second call).
- **Concurrent adds of the same product merge correctly, with no lost
  updates.** `CartService.addItem` serializes per-user (not globally), so
  20 threads adding 1x the same product concurrently end up as one line
  with quantity 20, not several partial/duplicate lines -- proven by
  `testConcurrentAddsOfSameProductMergeWithNoLostUpdates`.

## Reproducing the verified build locally

```bash
cd ecommerce-core
javac -Xlint:all -d out $(find src/main/java -name "*.java") $(find src/test/java -name "*.java")
java -cp out com.retailco.ecommerce.selftest.SelfTests   # 17/17, see reports/selftests-run-log.txt
java -cp out com.retailco.ecommerce.demo.Main             # see reports/demo-run-log.txt
```

## Reports (real output from this submission's own run)

- `reports/core-javac-lint-log.txt` -- `javac -Xlint:all`, clean (3 benign `serialVersionUID` warnings, same as elsewhere in this submission)
- `reports/selftests-run-log.txt` -- 17/17 tests passing, including real multi-threaded oversell and no-lost-updates tests
- `reports/demo-run-log.txt` -- a 3-turn narrative run: happy-path checkout, an oversell rejection, and a declined-payment rollback
