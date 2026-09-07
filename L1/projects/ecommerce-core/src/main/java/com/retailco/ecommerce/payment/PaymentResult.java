package com.retailco.ecommerce.payment;

// CONCEPT: Value object (record) with named factory methods
// (approved()/declined()) instead of a bare constructor -- makes call
// sites read clearly, e.g. `PaymentResult.declined("reason")`.
public record PaymentResult(boolean approved, String transactionId, String declineReason) {

    public static PaymentResult approved(String transactionId) {
        return new PaymentResult(true, transactionId, null);
    }

    public static PaymentResult declined(String reason) {
        return new PaymentResult(false, null, reason);
    }
}
