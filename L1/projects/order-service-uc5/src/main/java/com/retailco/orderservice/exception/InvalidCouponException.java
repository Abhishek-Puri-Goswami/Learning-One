package com.retailco.orderservice.exception;

/**
 * Thrown when a coupon code isn't one we recognize. An earlier version of
 * {@code DiscountCalculator} just silently ignored any coupon it didn't
 * know about and charged full price — which is a confusing experience for
 * a customer who typed "SAVE1O" (with a letter O) instead of "SAVE10" and
 * has no idea their coupon quietly did nothing. Throwing this exception
 * instead means the customer gets told clearly that their coupon code
 * wasn't valid.
 */
public class InvalidCouponException extends RuntimeException {
    public InvalidCouponException(String couponCode) {
        super("Coupon code is not valid or has expired: " + couponCode);
    }
}
