package com.retailco.orderreview.after;

import java.util.Set;

// CONCEPT: A small validator class with one job -- reject an unrecognized
// coupon code loudly instead of letting it silently do nothing.
/**
 * Closes a gap between L1/UC4's refactor and L1/UC5's edge-case catalog:
 * {@link AfterDiscountCalculator} (ported unchanged from UC4) silently
 * no-ops on ANY unrecognized coupon string, including a typo like "SAVE1O"
 * (letter O) -- which is exactly the "silently paying full price" failure
 * mode UC5's edge-case catalog calls out as wrong. This validator is meant
 * to run BEFORE {@link AfterDiscountCalculator}, in checkout order, so a
 * typo'd coupon fails loudly and before any payment attempt, while an
 * intentionally blank coupon field (customer didn't try to use one) is
 * correctly treated as "no coupon," not an error.
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
