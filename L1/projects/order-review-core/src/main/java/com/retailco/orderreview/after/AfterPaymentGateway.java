package com.retailco.orderreview.after;

import java.math.BigDecimal;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

/**
 * The fixed version of
 * {@link com.retailco.orderreview.before.BeforePaymentGateway}. Instead of
 * swallowing exceptions and always returning "success," any failure from
 * the gateway call is wrapped in a {@link PaymentFailedException} and
 * thrown — a payment failure can never again be mistaken for success.
 * <p>
 * This version also tells apart a timeout from every other kind of
 * failure, giving each one a different, more specific error message.
 * That distinction matters: someone debugging a production issue (or a
 * retry policy deciding whether to try again) needs to know "the network
 * was just slow" versus "the gateway actively rejected the charge" — very
 * different situations that call for very different responses.
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
