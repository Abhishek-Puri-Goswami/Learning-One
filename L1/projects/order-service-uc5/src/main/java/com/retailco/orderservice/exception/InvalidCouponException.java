package com.retailco.orderservice.exception;

// CONCEPT: Custom exception -- thrown for an unrecognized/expired coupon
// code, so it fails loudly instead of silently applying no discount.
/**
 * L1/UC5 fix (see edge-cases/edge-case-catalog.md, "Invalid coupon"): the
 * L1/UC4 DiscountCalculator silently ignored any coupon code it didn't
 * recognize (its `default -> lineTotal` branch), returning the full price
 * with no signal to the customer that "SAVE1O" (a typo) or an expired code
 * did nothing. That is a silent-failure UX/business bug -- the customer
 * believes a discount was applied and it wasn't. An unrecognized coupon now
 * throws this exception instead, surfaced to the client as a 400 with a
 * clear message.
 */
public class InvalidCouponException extends RuntimeException {
    public InvalidCouponException(String couponCode) {
        super("Coupon code is not valid or has expired: " + couponCode);
    }
}
