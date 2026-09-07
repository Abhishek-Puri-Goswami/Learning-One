package com.retailco.ecommerce.model;

/**
 * Order lifecycle used by {@link com.retailco.ecommerce.order.CheckoutService}.
 * Deliberately small -- this submission's L1/UC4 "order-service" refactor
 * scenario and L1/UC5 edge-case catalog only need enough states to express
 * "stock reserved, payment pending/settled/failed."
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    CANCELLED
}
