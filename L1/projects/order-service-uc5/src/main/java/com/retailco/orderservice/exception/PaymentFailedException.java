package com.retailco.orderservice.exception;

// CONCEPT: Custom exception wrapping a payment failure's original cause.
/**
 * FIX (was AI-SEC-2 / squid:S1166 + squid:S3516): PaymentGatewayClient now
 * throws this on any failure instead of swallowing the exception and
 * returning true unconditionally. Callers (OrderServiceImpl) must handle it
 * explicitly, so a failed charge can never be silently recorded as a
 * confirmed order.
 */
public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
