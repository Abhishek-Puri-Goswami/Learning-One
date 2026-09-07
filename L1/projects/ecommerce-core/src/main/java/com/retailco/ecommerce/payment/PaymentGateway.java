package com.retailco.ecommerce.payment;

import java.math.BigDecimal;

/**
 * A real payment gateway needs an external network call (Stripe/Razorpay/etc.)
 * this sandbox cannot make -- so this submission implements the interface only,
 * with a deterministic in-memory stub ({@link StubPaymentGateway}) as the sole
 * implementation, disclosed in this module's README. {@link com.retailco.ecommerce.order.CheckoutService}
 * is written against this interface, not the stub, so swapping in a real
 * gateway later is a one-class change.
 */
public interface PaymentGateway {
    PaymentResult charge(String orderId, String userId, BigDecimal amount);
}
