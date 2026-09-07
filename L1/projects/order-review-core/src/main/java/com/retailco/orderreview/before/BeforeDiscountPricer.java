package com.retailco.orderreview.before;

import java.math.BigDecimal;

/**
 * This is a snapshot of "messy" discount code, kept exactly as it
 * originally was — deeply nested {@code if/else} statements, several
 * levels deep. It's the same logic that used to live inline inside
 * {@code order-service-before}'s checkout method.
 * <p>
 * Notice we deliberately did NOT clean this up. That's the whole point:
 * by keeping this exact "before" version runnable, a test can compare its
 * output directly against the cleaned-up "after" version
 * ({@link com.retailco.orderreview.after.AfterDiscountCalculator}) and
 * prove they produce IDENTICAL results. That's how you can be confident a
 * refactor only changed the code's structure — not what it actually
 * computes.
 */
public final class BeforeDiscountPricer {

    private BeforeDiscountPricer() {
    }

    public static BigDecimal priceLine(BigDecimal lineTotal, String couponCode, String paymentMethod,
                                        String city, BigDecimal totalSoFar) {
        if (couponCode != null) {
            if (couponCode.equals("SAVE10")) {
                if (totalSoFar.compareTo(BigDecimal.valueOf(50)) > 0) {
                    if (paymentMethod.equals("card")) {
                        lineTotal = lineTotal.multiply(BigDecimal.valueOf(0.9));
                    } else {
                        if (paymentMethod.equals("upi")) {
                            lineTotal = lineTotal.multiply(BigDecimal.valueOf(0.92));
                        } else {
                            lineTotal = lineTotal.multiply(BigDecimal.valueOf(0.95));
                        }
                    }
                } else {
                    lineTotal = lineTotal.multiply(BigDecimal.valueOf(0.97));
                }
            } else if (couponCode.equals("VIP")) {
                if (city != null && city.equalsIgnoreCase("Bengaluru")) {
                    lineTotal = lineTotal.multiply(BigDecimal.valueOf(0.85));
                } else {
                    lineTotal = lineTotal.multiply(BigDecimal.valueOf(0.9));
                }
            }
        }
        return lineTotal;
    }
}
