package com.retailco.orderreview.before;

import java.math.BigDecimal;
import java.util.function.BiConsumer;

/**
 * Reproduces the exact bug AI review finding AI-SEC-2 describes in
 * {@code order-service-before}'s {@code PaymentGatewayClient.charge()}: any
 * exception from the underlying gateway call is swallowed by an empty catch
 * block, and the method returns {@code true} (payment succeeded)
 * unconditionally -- including on the failure path.
 *
 * <p>{@code gatewayCall} stands in for the real HTTP call
 * ({@code RestTemplate.postForObject}, blocked by Maven Central in this
 * sandbox); the bug being tested is in the surrounding control flow, not in
 * the HTTP client, so a pluggable failure-injecting stand-in is enough to
 * prove the defect is real, not just described.
 */
public final class BeforePaymentGateway {

    private BeforePaymentGateway() {
    }

    /** @return true always -- this is the bug under test, not a design choice. */
    public static boolean charge(BigDecimal amount, String paymentMethod, BiConsumer<BigDecimal, String> gatewayCall) {
        try {
            gatewayCall.accept(amount, paymentMethod);
        } catch (Exception e) {
            // FINDING AI-SEC-2: exception swallowed, no rethrow, no logging.
        }
        return true;
    }
}
