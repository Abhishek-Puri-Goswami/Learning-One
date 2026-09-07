package com.retailco.ecommerce.model;

/**
 * All the possible stages an order can be in: waiting for payment, paid,
 * payment failed, or cancelled. Using an enum here means the code can
 * never accidentally set an order's status to some typo'd or made-up
 * value — only one of these four states is allowed.
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    CANCELLED
}
