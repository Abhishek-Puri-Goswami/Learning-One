package com.retailco.ecommerce.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A fake, "pretend" payment gateway, used because we're not actually
 * connected to a real one like Stripe. It approves almost every charge —
 * except for two rules we built in on purpose, so we have a reliable way
 * to test what happens when a payment fails:
 * <ul>
 *   <li>Any charge above {@link #DECLINE_ABOVE} is declined, simulating a
 *       card that has hit its limit.</li>
 *   <li>Any charge made for the special test user {@link #FORCED_DECLINE_USER_ID}
 *       is always declined, no matter the amount — a predictable way to
 *       trigger the "payment failed" path in tests.</li>
 * </ul>
 */
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
