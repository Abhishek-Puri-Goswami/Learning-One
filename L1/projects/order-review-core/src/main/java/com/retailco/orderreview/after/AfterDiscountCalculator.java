package com.retailco.orderreview.after;

import java.math.BigDecimal;

/**
 * Pure-JDK port of {@code order-service-refactored}'s
 * {@code DiscountCalculator} (see
 * {@code ../../UC4-Code-Review-Quality-Governance/order-service-refactored/.../DiscountCalculator.java}),
 * with only the {@code @Component} annotation removed -- otherwise
 * unchanged, since the class had no Spring dependency to begin with. Each
 * coupon rule is its own small method instead of the nested if/else chain
 * in {@link com.retailco.orderreview.before.BeforeDiscountPricer}.
 */
public final class AfterDiscountCalculator {

    private static final BigDecimal SAVE10_THRESHOLD = BigDecimal.valueOf(50);

    public BigDecimal applyDiscount(BigDecimal lineTotal, String couponCode, String paymentMethod,
                                     String city, BigDecimal totalSoFar) {
        if (couponCode == null) {
            return lineTotal;
        }
        return switch (couponCode) {
            case "SAVE10" -> applySave10(lineTotal, paymentMethod, totalSoFar);
            case "VIP" -> applyVip(lineTotal, city);
            default -> lineTotal;
        };
    }

    private BigDecimal applySave10(BigDecimal lineTotal, String paymentMethod, BigDecimal totalSoFar) {
        if (totalSoFar.compareTo(SAVE10_THRESHOLD) <= 0) {
            return multiply(lineTotal, 0.97);
        }
        return switch (paymentMethod) {
            case "card" -> multiply(lineTotal, 0.90);
            case "upi" -> multiply(lineTotal, 0.92);
            default -> multiply(lineTotal, 0.95);
        };
    }

    private BigDecimal applyVip(BigDecimal lineTotal, String city) {
        boolean isBengaluru = city != null && city.equalsIgnoreCase("Bengaluru");
        return multiply(lineTotal, isBengaluru ? 0.85 : 0.90);
    }

    private BigDecimal multiply(BigDecimal amount, double factor) {
        return amount.multiply(BigDecimal.valueOf(factor));
    }
}
