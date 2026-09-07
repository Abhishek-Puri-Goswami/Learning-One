package com.retailco.ecommerce.payment;

import java.math.BigDecimal;

// CONCEPT: Strategy pattern via an interface -- defines what a payment
// gateway must do (charge an amount, return approved/declined), without
// saying HOW.
// WHY: CheckoutService depends only on this interface, not on
// StubPaymentGateway directly. A real payment provider (Stripe, etc.)
// could be swapped in later as a new implementation, with no change
// needed in CheckoutService.
public interface PaymentGateway {
    PaymentResult charge(String orderId, String userId, BigDecimal amount);
}
