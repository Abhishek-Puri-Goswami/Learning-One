package com.retailco.orderservice.exception;

/**
 * Thrown when a payment attempt fails. {@code PaymentGatewayClient} throws
 * this instead of quietly hiding the failure, which forces
 * {@code OrderServiceImpl} to actually deal with it — so a failed charge
 * can never be mistaken for a confirmed order.
 */
public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
