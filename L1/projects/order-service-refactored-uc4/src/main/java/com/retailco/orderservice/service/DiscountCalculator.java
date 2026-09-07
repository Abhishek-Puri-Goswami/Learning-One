package com.retailco.orderservice.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Works out the discount for one line of an order. This logic used to
 * live buried inside {@code OrderServiceImpl}'s giant {@code checkout()}
 * method, tangled up with everything else it was doing. Pulling it out
 * into its own class, with a small dedicated method per coupon type
 * ({@code applySave10}, {@code applyVip}), makes each pricing rule easy
 * to read and test completely on its own.
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
