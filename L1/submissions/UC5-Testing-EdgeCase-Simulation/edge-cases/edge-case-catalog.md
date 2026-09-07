# Edge Case Documentation — L1 UC5

Deliverable: "Edge case documentation," formatted per the use case's exact output spec: **Test name | Scenario | Expected result | Code snippet** (snippet references, full code lives in the linked file to avoid duplicating ~40 test methods here).

## CartService (`cart-service/src/test/java/.../CartServiceImplTest.java`)

| Test name | Scenario | Expected result | Code snippet (file : method) |
|---|---|---|---|
| `normalFlow_addSingleItem_returnsCartWithCorrectSubtotal` | Add 2x a $20 item to a fresh cart. | Cart has 1 line, subtotal = $40. | `CartServiceImplTest.java : normalFlow_addSingleItem_returnsCartWithCorrectSubtotal` |
| `emptyCart_forNewUser_returnsZeroItemsAndZeroSubtotal` | `GET` the cart for a user who has never added anything. | Cart auto-created with 0 items, $0 subtotal (not a 404). | `CartServiceImplTest.java : emptyCart_forNewUser_returnsZeroItemsAndZeroSubtotal` |
| `largeQuantity_atCap_isAccepted` | Add exactly 99 units (the configured cap) in one request. | Accepted — boundary value is inclusive. | `CartServiceImplTest.java : largeQuantity_atCap_isAccepted` |
| `largeQuantity_overCap_throwsInvalidQuantityException` | Add 999,999 units in one request. | Rejected with `InvalidQuantityException` → 400, not silently truncated or accepted. | `CartServiceImplTest.java : largeQuantity_overCap_throwsInvalidQuantityException` |
| `largeQuantity_sumAcrossTwoAdds_overCap_throwsOnSecondAdd` | Add 60, then add 60 more of the same product (sum = 120 > cap of 99). | Second call rejected — the cap applies to the running total per line, not just a single request. | `CartServiceImplTest.java : largeQuantity_sumAcrossTwoAdds_overCap_throwsOnSecondAdd` |
| `concurrentModification_sameProductAddedFromManyThreads_noLostUpdates` | 20 threads simultaneously add 1x of the same product to the same user's cart. | Final cart has exactly 1 line item with quantity = 20 (no lost updates, no duplicate lines). | `CartServiceImplTest.java : concurrentModification_sameProductAddedFromManyThreads_noLostUpdates` |
| `invalidProductId_productNotFoundInCatalog_throwsInvalidProductException` | Add a `productId` that returns 404 from product-service. | Rejected with `InvalidProductException` → 404. **Not** silently added at price $0 (the original bug). | `CartServiceImplTest.java : invalidProductId_productNotFoundInCatalog_throwsInvalidProductException` |

## Order Service — Payment & Checkout (`order-service/src/test/java/.../OrderServiceImplTest.java`, `PaymentGatewayClientTest.java`, `DiscountCalculatorTest.java`)

| Test name | Scenario | Expected result | Code snippet (file : method) |
|---|---|---|---|
| `checkout_happyPath_returnsConfirmedOrder` | Valid cart, valid address, successful payment. | Order status `CONFIRMED`, correct total, saved once. | `OrderServiceImplTest.java : checkout_happyPath_returnsConfirmedOrder` |
| `checkout_emptyCart_throwsWithoutCheckingStockOrPayment` | User checks out with an empty cart. | `EmptyCartException`; stock/payment never contacted. | `OrderServiceImplTest.java : checkout_emptyCart_throwsWithoutCheckingStockOrPayment` |
| `checkout_requestedQuantityExceedsAvailableStock_throwsOutOfStockBeforePayment` | **Out-of-stock**: cart wants 5 units, only 2 are in stock. | `OutOfStockException` → 409; payment is never attempted; order never saved. | `OrderServiceImplTest.java : checkout_requestedQuantityExceedsAvailableStock_throwsOutOfStockBeforePayment` |
| `checkout_stockExactlyMatchesRequestedQuantity_boundaryIsAccepted` | Requested quantity exactly equals available stock (boundary). | Order confirms — the "last unit" purchase succeeds. | `OrderServiceImplTest.java : checkout_stockExactlyMatchesRequestedQuantity_boundaryIsAccepted` |
| `checkout_paymentGatewayTimesOut_propagatesPaymentFailedException_orderNeverSaved` | **Payment timeout**: gateway call exceeds the configured read timeout. | `PaymentFailedException` with a "timed out" message; order is never saved as confirmed. | `OrderServiceImplTest.java : checkout_paymentGatewayTimesOut_propagatesPaymentFailedException_orderNeverSaved` |
| `charge_gatewayTimesOut_throwsPaymentFailedExceptionWithTimeoutMessage` | Client-level: `RestTemplate` throws `ResourceAccessException` wrapping a `SocketTimeoutException`. | `PaymentFailedException` distinguishing "timed out" from a generic failure. | `PaymentGatewayClientTest.java : charge_gatewayTimesOut_...` |
| `charge_gatewayReturns5xx_throwsPaymentFailedExceptionWithGenericMessage` | Client-level: gateway returns HTTP 500. | `PaymentFailedException` with a generic failure message. | `PaymentGatewayClientTest.java : charge_gatewayReturns5xx_...` |
| `checkout_unrecognizedCoupon_throwsInvalidCouponException_beforePayment` | **Invalid coupon**: customer enters `SAVE1O` (letter O, typo for `SAVE10`). | `InvalidCouponException` → 400 *before* any payment attempt; customer is told the coupon didn't work instead of silently paying full price. | `OrderServiceImplTest.java : checkout_unrecognizedCoupon_throwsInvalidCouponException_beforePayment` |
| `checkout_validCoupon_appliesDiscountAndConfirms` | Valid `SAVE10` coupon on a cart above the discount threshold, paid by card. | 10% discount applied; order confirms at the discounted total. | `OrderServiceImplTest.java : checkout_validCoupon_appliesDiscountAndConfirms` |
| `unknownCoupon_throwsInvalidCouponException` / `expiredOrRetiredCoupon_throwsInvalidCouponException` | Unit-level: `DiscountCalculator` given an unregistered coupon code. | `InvalidCouponException`, not a silent no-op discount. | `DiscountCalculatorTest.java` |
| `blankCouponCode_treatedAsNoCoupon_notAnError` | Customer submits an empty coupon field (didn't intend to use one). | No discount, no error — distinguished from *entering* an invalid code. | `DiscountCalculatorTest.java : blankCouponCode_treatedAsNoCoupon_notAnError` |

## Security Scenario Test

Deliverable: "Security scenario test." `PaymentGatewayClientTest` doubles as this — it proves that a hung or erroring payment gateway call can **never** result in an order being silently confirmed (the exact vulnerability fixed in L1/UC4's `AI-SEC-2` and re-verified here under the specific timeout condition). Combined with `checkout_paymentGatewayTimesOut_...` in `OrderServiceImplTest`, this closes the loop from "gateway hangs" all the way up to "customer-visible checkout response" with no path where a failed/timed-out charge is misreported as success.

## Summary: Brief's Required Simulations, Traced to Tests

| Required simulation | Test(s) |
|---|---|
| Out-of-stock | `checkout_requestedQuantityExceedsAvailableStock_throwsOutOfStockBeforePayment`, `checkout_stockExactlyMatchesRequestedQuantity_boundaryIsAccepted` |
| Payment timeout | `checkout_paymentGatewayTimesOut_...`, `charge_gatewayTimesOut_...` |
| Invalid coupon | `checkout_unrecognizedCoupon_...`, `unknownCoupon_throwsInvalidCouponException`, `expiredOrRetiredCoupon_...` |
