package com.retailco.ecommerce.model;

// CONCEPT: Enum -- a fixed, closed set of possible order states.
// PURPOSE: Tracks where an order is in its lifecycle: waiting on payment,
// paid, payment failed, or cancelled.
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    CANCELLED
}
