package com.retailco.orderreview.selftest;

import com.retailco.orderreview.after.AfterCheckoutFlow;
import com.retailco.orderreview.after.AfterDiscountCalculator;
import com.retailco.orderreview.after.AfterPaymentGateway;
import com.retailco.orderreview.after.CouponPolicy;
import com.retailco.orderreview.after.InvalidCouponException;
import com.retailco.orderreview.after.PaymentFailedException;
import com.retailco.orderreview.before.BeforeCheckoutFlow;
import com.retailco.orderreview.before.BeforeDiscountPricer;
import com.retailco.orderreview.before.BeforePaymentGateway;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hand-rolled self-test harness (no JUnit -- Maven Central is blocked, same
 * constraint as every other module in this submission). Ties every finding
 * in {@code ../../UC4-Code-Review-Quality-Governance/reviews/ai-review-report.json}
 * that has a code-level fix to a real, executed test against real (compiled
 * and run) code -- both the "before" bug it reproduces and the "after" fix
 * it confirms. Run with:
 *   java -cp out com.retailco.orderreview.selftest.SelfTests
 */
public final class SelfTests {

    private static final AtomicInteger PASS = new AtomicInteger();
    private static final AtomicInteger FAIL = new AtomicInteger();

    public static void main(String[] args) {
        testRefactorPreservesDiscountBehaviorAcrossAllBranches();
        testBeforePaymentBugReportsSuccessOnFailure();      // AI-SEC-2
        testAfterPaymentFixPropagatesFailure();              // AI-SEC-2 fix
        testAfterPaymentSucceedsSilentlyOnRealSuccess();
        testBeforeNullShippingAddressThrowsUnguardedNpe();   // AI-QA-1
        testAfterNullShippingAddressFailsWithClearError();   // AI-QA-1 fix
        testUnrecognizedCouponRejectedBeforePayment();
        testBlankCouponTreatedAsNoCouponNotAnError();
        testRecognizedCouponPassesValidation();
        testPaymentTimeoutDistinguishedFromGenericFailure();

        System.out.println();
        System.out.println("Results: " + PASS.get() + " passed, " + FAIL.get() + " failed");
        if (FAIL.get() > 0) {
            System.exit(1);
        }
    }

    /**
     * The single most important test in this suite: proves the refactor
     * (nested if/else -> DiscountCalculator with a switch expression) is a
     * pure structural change, not a behavior change, across every coupon /
     * payment-method / city / running-total combination the original logic
     * branches on.
     */
    private static void testRefactorPreservesDiscountBehaviorAcrossAllBranches() {
        String name = "refactor preserves discount behavior: before/after produce identical totals across all branches";
        try {
            record Case(String coupon, String paymentMethod, String city, BigDecimal totalSoFar) {
            }
            List<Case> cases = List.of(
                    new Case(null, "card", "Mumbai", BigDecimal.ZERO),
                    new Case("SAVE10", "card", "Mumbai", BigDecimal.valueOf(20)),   // below threshold
                    new Case("SAVE10", "card", "Mumbai", BigDecimal.valueOf(80)),   // above threshold, card
                    new Case("SAVE10", "upi", "Mumbai", BigDecimal.valueOf(80)),    // above threshold, upi
                    new Case("SAVE10", "cash", "Mumbai", BigDecimal.valueOf(80)),   // above threshold, other
                    new Case("VIP", "card", "Bengaluru", BigDecimal.valueOf(10)),
                    new Case("VIP", "card", "Mumbai", BigDecimal.valueOf(10)),
                    new Case("VIP", "card", null, BigDecimal.valueOf(10)),
                    new Case("UNKNOWN10", "card", "Mumbai", BigDecimal.valueOf(80))
            );

            AfterDiscountCalculator after = new AfterDiscountCalculator();
            BigDecimal lineTotal = new BigDecimal("199.00");
            boolean allMatch = true;
            for (Case c : cases) {
                BigDecimal beforeResult = BeforeDiscountPricer.priceLine(
                        lineTotal, c.coupon(), c.paymentMethod(), c.city(), c.totalSoFar());
                BigDecimal afterResult = after.applyDiscount(
                        lineTotal, c.coupon(), c.paymentMethod(), c.city(), c.totalSoFar());
                if (beforeResult.compareTo(afterResult) != 0) {
                    allMatch = false;
                    System.out.println("    MISMATCH for " + c + ": before=" + beforeResult + " after=" + afterResult);
                }
            }
            check(name, allMatch);
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testBeforePaymentBugReportsSuccessOnFailure() {
        String name = "AI-SEC-2 (before): a failing gateway call is still reported as a successful charge";
        try {
            boolean reportedSuccess = BeforePaymentGateway.charge(
                    new BigDecimal("50.00"), "card",
                    (amount, method) -> { throw new RuntimeException("simulated gateway outage"); });
            check(name, reportedSuccess); // this being true IS the bug -- proves it's real, not hypothetical
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testAfterPaymentFixPropagatesFailure() {
        String name = "AI-SEC-2 (after): a failing gateway call throws PaymentFailedException, never reports success";
        try {
            boolean threw = false;
            try {
                AfterPaymentGateway.charge(new BigDecimal("50.00"), "card",
                        (amount, method) -> { throw new RuntimeException("simulated gateway outage"); });
            } catch (PaymentFailedException e) {
                threw = true;
            }
            check(name, threw);
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testAfterPaymentSucceedsSilentlyOnRealSuccess() {
        String name = "AI-SEC-2 (after): a genuinely successful gateway call does not throw";
        try {
            AtomicInteger callCount = new AtomicInteger();
            AfterPaymentGateway.charge(new BigDecimal("50.00"), "card",
                    (amount, method) -> callCount.incrementAndGet());
            check(name, callCount.get() == 1);
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testBeforeNullShippingAddressThrowsUnguardedNpe() {
        String name = "AI-QA-1 (before): a null shippingAddress throws an unguarded NullPointerException";
        try {
            boolean threwNpe = false;
            try {
                BeforeCheckoutFlow.resolveCity(null);
            } catch (NullPointerException e) {
                threwNpe = true;
            }
            check(name, threwNpe);
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testAfterNullShippingAddressFailsWithClearError() {
        String name = "AI-QA-1 (after): a null shippingAddress fails with a named validation error, not a bare NPE";
        try {
            boolean threwIllegalArgument = false;
            try {
                AfterCheckoutFlow.resolveCity(null);
            } catch (IllegalArgumentException e) {
                threwIllegalArgument = true;
            }
            check(name, threwIllegalArgument);
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testUnrecognizedCouponRejectedBeforePayment() {
        String name = "unrecognized coupon code (typo 'SAVE1O') is rejected with InvalidCouponException";
        try {
            boolean threw = false;
            try {
                CouponPolicy.validate("SAVE1O"); // letter O, not zero -- a typo for SAVE10
            } catch (InvalidCouponException e) {
                threw = true;
            }
            check(name, threw);
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testBlankCouponTreatedAsNoCouponNotAnError() {
        String name = "blank/absent coupon code is treated as 'no coupon', not an error";
        try {
            CouponPolicy.validate(null);
            CouponPolicy.validate("");
            CouponPolicy.validate("   ");
            check(name, true); // no exception thrown for any of the three -- that's the assertion
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testRecognizedCouponPassesValidation() {
        String name = "recognized coupon codes (SAVE10, VIP) pass validation without error";
        try {
            CouponPolicy.validate("SAVE10");
            CouponPolicy.validate("VIP");
            check(name, true);
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testPaymentTimeoutDistinguishedFromGenericFailure() {
        String name = "payment gateway timeout produces a distinct 'timed out' message vs a generic gateway failure";
        try {
            String timeoutMessage = null;
            try {
                AfterPaymentGateway.charge(new BigDecimal("10.00"), "card", (amount, method) -> {
                    throw new RuntimeException(new TimeoutException("read timed out after 5000ms"));
                });
            } catch (PaymentFailedException e) {
                timeoutMessage = e.getMessage();
            }

            String genericMessage = null;
            try {
                AfterPaymentGateway.charge(new BigDecimal("10.00"), "card", (amount, method) -> {
                    throw new RuntimeException("HTTP 500 from gateway");
                });
            } catch (PaymentFailedException e) {
                genericMessage = e.getMessage();
            }

            check(name, timeoutMessage != null && timeoutMessage.contains("timed out")
                    && genericMessage != null && !genericMessage.contains("timed out"));
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            PASS.incrementAndGet();
            System.out.println("[PASS] " + name);
        } else {
            FAIL.incrementAndGet();
            System.out.println("[FAIL] " + name + " -- condition was false");
        }
    }

    private static void fail(String name, Exception e) {
        FAIL.incrementAndGet();
        System.out.println("[FAIL] " + name + " -- threw " + e.getClass().getSimpleName() + ": " + e.getMessage());
    }
}
