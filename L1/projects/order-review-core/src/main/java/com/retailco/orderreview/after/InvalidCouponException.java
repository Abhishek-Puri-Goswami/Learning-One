package com.retailco.orderreview.after;

/**
 * Thrown when a coupon code isn't one we recognize — for example, a typo
 * like "SAVE1O" (letter O instead of zero). This should be rejected with
 * a clear error before payment is attempted, rather than silently
 * charging full price with no explanation to the customer. See
 * {@link CouponPolicy#validate} for where this gets thrown.
 */
public class InvalidCouponException extends RuntimeException {
    public InvalidCouponException(String couponCode) {
        super("unrecognized coupon code: '" + couponCode + "'");
    }
}
