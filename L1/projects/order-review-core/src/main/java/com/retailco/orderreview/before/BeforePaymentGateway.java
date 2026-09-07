package com.retailco.orderreview.before;

import java.math.BigDecimal;
import java.util.function.BiConsumer;

/**
 * A tiny, standalone example of a real bug from
 * {@code order-service-before}'s {@code PaymentGatewayClient.charge()}:
 * if the underlying payment call throws an exception, this method catches
 * it and does... nothing. It still returns {@code true} ("payment
 * succeeded") no matter what — even when the payment actually failed!
 * <p>
 * {@code gatewayCall} here stands in for the real network call to a
 * payment provider. What we're testing is the surrounding logic — the
 * fact that an error gets swallowed and ignored — not the network call
 * itself, so a simple pluggable stand-in is all we need to prove the bug
 * is real.
 */
public final class BeforePaymentGateway {

    private BeforePaymentGateway() {
    }

    /** @return always {@code true} — that's the bug this class demonstrates, not intended behavior. */
    public static boolean charge(BigDecimal amount, String paymentMethod, BiConsumer<BigDecimal, String> gatewayCall) {
        try {
            gatewayCall.accept(amount, paymentMethod);
        } catch (Exception e) {
            // This is the bug: the exception is caught here and simply
            // thrown away — nothing is logged, nothing is re-thrown, and
            // the method still reports success below regardless.
        }
        return true;
    }
}
