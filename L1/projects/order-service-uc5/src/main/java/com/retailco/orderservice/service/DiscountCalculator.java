package com.retailco.orderservice.service;

import com.retailco.orderservice.exception.InvalidCouponException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Works out the discount for one line of an order. This version keeps an
 * explicit list of coupon codes we actually recognize
 * ({@code KNOWN_COUPONS}) and throws {@link InvalidCouponException} for
 * anything not on that list, instead of quietly applying no discount.
 * That way a mistyped or expired coupon code is caught and reported
 * clearly, rather than silently doing nothing while the customer assumes
 * it worked.
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
            default -> lineTotal; // can't actually happen -- the check above already rejects anything unrecognized
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
