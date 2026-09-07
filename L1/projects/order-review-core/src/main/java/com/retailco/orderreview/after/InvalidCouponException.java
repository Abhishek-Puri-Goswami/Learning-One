package com.retailco.orderreview.after;

/**
 * Mirrors L1/UC5's edge-case catalog: an unrecognized coupon code (e.g. a
 * typo, "SAVE1O" with a letter O instead of zero) must be rejected with a
 * clear error BEFORE payment is attempted -- not silently treated as "no
 * discount" and charged at full price with no explanation. A blank/absent
 * coupon code is a different case entirely (the customer didn't intend to
 * use one) and is NOT an error -- see {@link CouponPolicy#validate}.
 */
public class InvalidCouponException extends RuntimeException {
    public InvalidCouponException(String couponCode) {
        super("unrecognized coupon code: '" + couponCode + "'");
    }
}
