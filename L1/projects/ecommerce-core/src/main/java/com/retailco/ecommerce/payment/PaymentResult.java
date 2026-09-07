package com.retailco.ecommerce.payment;

/**
 * The outcome of a payment attempt. Instead of writing
 * {@code new PaymentResult(true, id, null)} everywhere (which is hard to
 * read — what does {@code true} mean here?), we provide two named helper
 * methods below, {@code approved(...)} and {@code declined(...)}, so the
 * calling code reads clearly, like {@code PaymentResult.declined("card expired")}.
 */
public record PaymentResult(boolean approved, String transactionId, String declineReason) {

    public static PaymentResult approved(String transactionId) {
        return new PaymentResult(true, transactionId, null);
    }

    public static PaymentResult declined(String reason) {
        return new PaymentResult(false, null, reason);
    }
}
