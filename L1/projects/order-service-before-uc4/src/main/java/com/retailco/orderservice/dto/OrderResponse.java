package com.retailco.orderservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderResponse {
    private String id;
    private String status;
    private BigDecimal total;
    private List<String> lineSummaries;
    private Instant createdAt;

    public OrderResponse() {
    }

    public OrderResponse(String id, String status, BigDecimal total, List<String> lineSummaries, Instant createdAt) {
        this.id = id;
        this.status = status;
        this.total = total;
        this.lineSummaries = lineSummaries;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public List<String> getLineSummaries() { return lineSummaries; }
    public void setLineSummaries(List<String> lineSummaries) { this.lineSummaries = lineSummaries; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
