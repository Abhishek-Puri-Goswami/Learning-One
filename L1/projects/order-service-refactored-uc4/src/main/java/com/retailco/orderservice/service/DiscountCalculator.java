package com.retailco.orderservice.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// CONCEPT: Extracting a helper class to reduce complexity -- each pricing
// rule (SAVE10, VIP) gets its own small method instead of one giant
// method with nested if/else for everything.
/**
 * FIX (part of resolving AI-QA "high complexity" / squid:S3776, squid:S138):
 * extracted from the original ~65-line checkout() method. Each coupon rule
 * is now its own small, independently testable method instead of a chain of
 * nested if/else blocks combining coupon + payment method + city + running
 * total in one branch tree.
 */
@Component
public class DiscountCalculator {

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
            return multiply(lineTotal, 0.97); // below threshold: flat small discount
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
