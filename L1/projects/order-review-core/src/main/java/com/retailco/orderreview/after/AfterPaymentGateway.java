package com.retailco.orderreview.after;

import java.math.BigDecimal;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

/**
 * Pure-JDK port of {@code order-service-refactored}'s fixed
 * {@code PaymentGatewayClient.charge()} -- the FIX for AI-SEC-2: any
 * exception from the gateway call is wrapped in {@link PaymentFailedException}
 * and propagated, instead of being swallowed and reported as success (see
 * {@link com.retailco.orderreview.before.BeforePaymentGateway}, the bug this
 * replaces).
 *
 * <p>Extended for L1/UC5's edge-case catalog: a timeout is distinguished
 * from a generic gateway failure in the exception message (mirrors
 * {@code PaymentGatewayClientTest.charge_gatewayTimesOut_...} vs
 * {@code charge_gatewayReturns5xx_...} in the real UC5 test suite) --
 * useful for an on-call engineer or a retry policy that behaves differently
 * for "the network was slow" versus "the gateway rejected the charge."
 */
public final class AfterPaymentGateway {

    private AfterPaymentGateway() {
    }

    /**
     * @throws PaymentFailedException if gatewayCall throws -- never returns a false "success."
     *         The message says "timed out" specifically when the underlying cause is a
     *         {@link TimeoutException} (this module's stand-in for Spring's
     *         {@code ResourceAccessException} wrapping a {@code SocketTimeoutException}),
     *         and a generic failure message otherwise (e.g. the gateway returning HTTP 5xx).
     */
    public static void charge(BigDecimal amount, String paymentMethod, BiConsumer<BigDecimal, String> gatewayCall) {
        try {
            gatewayCall.accept(amount, paymentMethod);
        } catch (Exception e) {
            if (e instanceof TimeoutException || e.getCause() instanceof TimeoutException) {
                throw new PaymentFailedException(
                        "Payment gateway call timed out for amount " + amount + " via " + paymentMethod, e);
            }
            throw new PaymentFailedException(
                    "Payment gateway call failed for amount " + amount + " via " + paymentMethod, e);
        }
    }
}
