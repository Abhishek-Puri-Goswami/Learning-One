package com.retailco.ecommerce.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Result of a committed checkout ({@link com.retailco.ecommerce.order.CheckoutService#checkout}).
 * Mutable only in {@code status} (set by {@link com.retailco.ecommerce.order.CheckoutService}
 * after the payment gateway responds) -- lines and total are frozen at creation.
 */
public class Order {

    private final String orderId;
    private final String userId;
    private final List<OrderLine> lines;
    private final BigDecimal total;
    private final Instant createdAt;
    private OrderStatus status;

    public Order(String orderId, String userId, List<OrderLine> lines, BigDecimal total, Instant createdAt) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("an order must have at least one line");
        }
        this.orderId = orderId;
        this.userId = userId;
        this.lines = List.copyOf(lines);
        this.total = total;
        this.createdAt = createdAt;
        this.status = OrderStatus.PENDING_PAYMENT;
    }

    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public List<OrderLine> getLines() { return lines; }
    public BigDecimal getTotal() { return total; }
    public Instant getCreatedAt() { return createdAt; }
    public OrderStatus getStatus() { return status; }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Order[orderId=" + orderId + ", userId=" + userId + ", lines=" + lines.size()
                + ", total=" + total + ", status=" + status + "]";
    }
}
