package com.retailco.ecommerce.payment;

import java.math.BigDecimal;
import java.util.UUID;

// CONCEPT: Fake/stub implementation of an interface, used because there's
// no real external payment provider connected.
// PURPOSE: Always approves, EXCEPT for two deterministic rules used to
// test edge cases on purpose: charges above DECLINE_ABOVE are declined
// (simulates a card limit), and any charge for FORCED_DECLINE_USER_ID is
// always declined (a fixed way to test "what happens when payment fails").
public class StubPaymentGateway implements PaymentGateway {

    public static final BigDecimal DECLINE_ABOVE = new BigDecimal("10000.00");
    public static final String FORCED_DECLINE_USER_ID = "USER_FORCED_DECLINE";

    @Override
    public PaymentResult charge(String orderId, String userId, BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("amount must be non-negative: " + amount);
        }
        if (FORCED_DECLINE_USER_ID.equals(userId)) {
            return PaymentResult.declined("card declined by issuer (test sentinel user)");
        }
        if (amount.compareTo(DECLINE_ABOVE) > 0) {
            return PaymentResult.declined("card limit exceeded: charge " + amount + " > limit " + DECLINE_ABOVE);
        }
        return PaymentResult.approved("TXN-" + UUID.randomUUID());
    }
}
