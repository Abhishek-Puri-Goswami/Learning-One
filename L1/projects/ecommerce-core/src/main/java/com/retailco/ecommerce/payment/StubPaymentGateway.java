package com.retailco.ecommerce.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Deterministic stand-in for a real payment gateway (disclosed in this
 * module's README as illustrative-only, same disclosure pattern as this
 * submission's other external-dependency stubs -- e.g. L3's mock
 * calendar/inbox tools). Two deterministic decline rules, both needed by
 * L1/UC5's edge-case catalog:
 *
 * <ol>
 *   <li>Any charge over {@link #DECLINE_ABOVE} is declined ("card limit exceeded"),
 *       so a large-order edge case is reproducible without randomness.</li>
 *   <li>Any charge for the sentinel user {@link #FORCED_DECLINE_USER_ID} is declined
 *       regardless of amount, so tests can force the "payment fails after stock
 *       was already reserved" path deterministically.</li>
 * </ol>
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
