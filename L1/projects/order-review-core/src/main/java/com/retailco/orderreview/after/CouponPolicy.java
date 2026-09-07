package com.retailco.orderreview.after;

import java.util.Set;

/**
 * A small class with exactly one job: reject a coupon code we don't
 * recognize, loudly and clearly. {@link AfterDiscountCalculator} on its
 * own would silently do nothing for an unrecognized coupon like a typo'd
 * "SAVE1O" (letter O instead of zero) — this class is meant to run
 * BEFORE the discount calculator, so a bad coupon code is caught and
 * rejected right away, before payment is even attempted. A blank or
 * missing coupon field is treated differently — that just means the
 * customer didn't try to use one at all, which isn't an error.
 */
public final class CouponPolicy {

    private static final Set<String> RECOGNIZED_CODES = Set.of("SAVE10", "VIP");

    private CouponPolicy() {
    }

    /**
     * @param couponCode null or blank means "customer did not enter a coupon" -- not an error.
     * @throws InvalidCouponException if couponCode is non-blank but not a recognized code.
     */
    public static void validate(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return; // no coupon entered -- valid, not an error
        }
        if (!RECOGNIZED_CODES.contains(couponCode)) {
            throw new InvalidCouponException(couponCode);
        }
    }
}
