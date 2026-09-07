package com.retailco.orderreview.before;

import java.math.BigDecimal;

// CONCEPT: A "before" snapshot of messy code (deep nested if/else) kept
// runnable on purpose, so a test can prove the "after" refactor computes
// the exact same results despite being restructured.
/**
 * A pure-JDK, behavior-preserving port of the nested if/else discount logic
 * that lived inline inside {@code order-service-before}'s
 * {@code OrderServiceImpl.checkout()} (see
 * {@code ../../UC4-Code-Review-Quality-Governance/order-service-before/.../OrderServiceImpl.java},
 * the block AI review finding "high cyclomatic/cognitive complexity" points
 * at). Stripped of Spring/HTTP concerns so the discount arithmetic itself --
 * the part the review actually complains about -- can be compiled and run
 * for real, rather than only asserted correct in a report.
 *
 * <p>This is intentionally an almost line-for-line copy of the original
 * nested-conditional shape (not cleaned up) -- the point of this class is
 * to reproduce the "before" behavior faithfully so
 * {@code SelfTests.testRefactorPreservesDiscountBehavior} can prove the
 * "after" {@link com.retailco.orderreview.after.AfterDiscountCalculator}
 * computes IDENTICAL totals, i.e. the refactor changed structure without
 * changing behavior.
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
