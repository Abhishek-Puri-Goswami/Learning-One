package com.retailco.ecommerce.payment;

import java.math.BigDecimal;

/**
 * This interface just says "any payment gateway must be able to charge an
 * amount and tell us if it worked" — it doesn't say HOW that happens.
 * {@code CheckoutService} only ever talks to this interface, never to a
 * specific gateway directly. That means we can swap in a real provider
 * like Stripe or Razorpay later just by writing a new class that
 * implements this interface — {@code CheckoutService} wouldn't need to
 * change at all. This is a common design idea called the "Strategy
 * pattern."
 */
public interface PaymentGateway {
    PaymentResult charge(String orderId, String userId, BigDecimal amount);
}
