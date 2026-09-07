package com.retailco.orderreview.after;

/**
 * Thrown when a payment attempt fails, carrying the original cause along
 * with it. This mirrors {@code order-service-refactored}'s own
 * {@code PaymentFailedException} — it's just a standalone copy here so
 * {@link AfterPaymentGateway} can run without needing all of Spring Boot.
 */
public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
