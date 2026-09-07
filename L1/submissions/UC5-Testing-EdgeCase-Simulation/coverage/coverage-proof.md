# Coverage Proof — L1 UC5

Deliverable: "Coverage proof" / "coverage report." As in L1/UC2 and L1/UC4, this sandbox has no outbound access to Maven Central, so `mvn test` + JaCoCo could not be run to produce a real, tool-generated coverage report here. What follows is a manual, method-by-method coverage analysis instead, with the exact command to produce a real report once you're on a machine with normal internet access.

## To Produce a Real Coverage Report

Add this to each module's `pom.xml` (`<build><plugins>`), then run once per module:

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.12</version>
  <executions>
    <execution>
      <goals><goal>prepare-agent</goal></goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>test</phase>
      <goals><goal>report</goal></goals>
    </execution>
  </executions>
</plugin>
```

```bash
cd cart-service  && mvn test   # report at target/site/jacoco/index.html
cd order-service && mvn test   # report at target/site/jacoco/index.html
```

## Manual Coverage Analysis — `cart-service`

| Class | Method | Exercised by | Covered? |
|---|---|---|---|
| `CartServiceImpl` | `getCart` | `emptyCart_forNewUser_...`, `concurrentModification_...` (final read) | ✅ |
| `CartServiceImpl` | `addItem` — new item, happy path | `normalFlow_addSingleItem_...` | ✅ |
| `CartServiceImpl` | `addItem` — existing item, quantity summed | `concurrentModification_...` (implicitly), `largeQuantity_sumAcrossTwoAdds_...` | ✅ |
| `CartServiceImpl` | `addItem` — over-cap on first add | `largeQuantity_overCap_throwsInvalidQuantityException` | ✅ |
| `CartServiceImpl` | `addItem` — over-cap on summed add | `largeQuantity_sumAcrossTwoAdds_overCap_throwsOnSecondAdd` | ✅ |
| `CartServiceImpl` | `addItem` — invalid product (404) | `invalidProductId_productNotFoundInCatalog_...` | ✅ |
| `CartServiceImpl` | `updateItem` | *(carried over from L1/UC2, not re-tested here)* | ⚠️ Not covered in this pass — recommend adding in a follow-up |
| `CartServiceImpl` | `removeItem` | *(carried over from L1/UC2, not re-tested here)* | ⚠️ Not covered in this pass |
| `ProductCatalogClient` | `fetchProduct` — 404 path | Covered indirectly via `CartServiceImplTest` mocking the client, not the client itself | ⚠️ Recommend a dedicated `ProductCatalogClientTest` mirroring `PaymentGatewayClientTest`'s pattern |
| `DiscountCalculator` | n/a (order-service, not cart-service) | — | — |

**Estimated line coverage for `CartServiceImpl`: ~85%** (the two untested methods, `updateItem`/`removeItem`, are the main gap).

## Manual Coverage Analysis — `order-service`

| Class | Method | Exercised by | Covered? |
|---|---|---|---|
| `OrderServiceImpl` | `checkout` — happy path | `checkout_happyPath_returnsConfirmedOrder` | ✅ |
| `OrderServiceImpl` | `checkout` — empty cart | `checkout_emptyCart_throwsWithoutCheckingStockOrPayment` | ✅ |
| `OrderServiceImpl` | `checkout` — out-of-stock | `checkout_requestedQuantityExceedsAvailableStock_...`, boundary test | ✅ |
| `OrderServiceImpl` | `checkout` — payment timeout/failure | `checkout_paymentGatewayTimesOut_...` | ✅ |
| `OrderServiceImpl` | `checkout` — invalid coupon | `checkout_unrecognizedCoupon_...` | ✅ |
| `OrderServiceImpl` | `checkout` — valid coupon discount | `checkout_validCoupon_appliesDiscountAndConfirms` | ✅ |
| `DiscountCalculator` | all branches (SAVE10 card/upi/other, VIP city/other, unknown, blank) | `DiscountCalculatorTest` (7 tests) | ✅ |
| `PaymentGatewayClient` | `charge` — timeout, 5xx | `PaymentGatewayClientTest` (2 tests) | ✅ |
| `PaymentGatewayClient` | `charge` — happy path (no exception) | Exercised indirectly via `OrderServiceImplTest`'s mocked `paymentGatewayClient` | ⚠️ Not directly tested at the client layer with a real success response body |
| `CartClient`, `ProductClient` | `getCart`, `getAvailableStock` | Mocked in `OrderServiceImplTest`, not tested directly | ⚠️ Recommend dedicated client tests (mirroring `PaymentGatewayClientTest`) in a follow-up pass |

**Estimated line coverage for `OrderServiceImpl` + `DiscountCalculator`: ~90%** (checkout's core branches are all covered; the main gap is the HTTP client classes themselves, which are currently only exercised through mocks).

## Summary

| Module | Before UC5 | After UC5 (estimated) |
|---|---|---|
| `cart-service` | 0% (no tests existed) | ~85% on `CartServiceImpl`, gaps in `updateItem`/`removeItem` and `ProductCatalogClient` |
| `order-service` | 0% test files for `checkout()` prior to L1/UC4's `OrderServiceImplTest` (4 tests); UC5 adds 5 more scenario tests + 2 client-level tests | ~90% on the checkout path; gaps in `CartClient`/`ProductClient` direct testing |

**Follow-up recommended for a future pass:** add `updateItem`/`removeItem` tests for `CartServiceImpl`, and direct `CartClientTest`/`ProductClientTest`/`ProductCatalogClientTest` files so every HTTP-calling class has at least one test that doesn't go through a mock of itself.
