package com.retailco.orderservice.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class Order {

    private String id;
    private String userId;
    private String idempotencyKey;
    private List<OrderLine> lines;
    private ShippingAddress shippingAddress;
    private String paymentMethod;
    private BigDecimal total;
    private String status;
    private Instant createdAt;

    public Order() {
    }

    public Order(String id, String userId, String idempotencyKey, List<OrderLine> lines,
                 ShippingAddress shippingAddress, String paymentMethod, BigDecimal total,
                 String status, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.lines = lines;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public List<OrderLine> getLines() { return lines; }
    public void setLines(List<OrderLine> lines) { this.lines = lines; }

    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(ShippingAddress shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
