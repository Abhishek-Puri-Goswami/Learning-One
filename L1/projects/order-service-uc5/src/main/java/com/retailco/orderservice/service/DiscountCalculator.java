package com.retailco.orderservice.service;

import com.retailco.orderservice.exception.InvalidCouponException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Carried forward from L1/UC4 (extracted from checkout() to reduce
 * complexity). L1/UC5 fix (see edge-cases/edge-case-catalog.md, "Invalid
 * coupon"): an unrecognized coupon code now throws InvalidCouponException
 * instead of silently falling through to "no discount applied" -- the
 * previous behavior let a typo'd or expired code fail silently, which looks
 * to the customer like the coupon worked when it didn't.
 */
@Component
public class DiscountCalculator {

    private static final Set<String> KNOWN_COUPONS = Set.of("SAVE10", "VIP");
    private static final BigDecimal SAVE10_THRESHOLD = BigDecimal.valueOf(50);

    public BigDecimal applyDiscount(BigDecimal lineTotal, String couponCode, String paymentMethod,
                                     String city, BigDecimal totalSoFar) {
        if (couponCode == null || couponCode.isBlank()) {
            return lineTotal;
        }
        if (!KNOWN_COUPONS.contains(couponCode)) {
            throw new InvalidCouponException(couponCode);
        }
        return switch (couponCode) {
            case "SAVE10" -> applySave10(lineTotal, paymentMethod, totalSoFar);
            case "VIP" -> applyVip(lineTotal, city);
            default -> lineTotal; // unreachable given the KNOWN_COUPONS guard above
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
