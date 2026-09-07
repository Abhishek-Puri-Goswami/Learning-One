package com.retailco.orderreview.after;

// CONCEPT: Custom exception wrapping the original payment failure cause.
/**
 * Pure-JDK mirror of {@code order-service-refactored}'s
 * {@code exception.PaymentFailedException} -- unchanged in shape, just
 * relocated so {@link AfterPaymentGateway} can be compiled without Spring.
 */
public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
