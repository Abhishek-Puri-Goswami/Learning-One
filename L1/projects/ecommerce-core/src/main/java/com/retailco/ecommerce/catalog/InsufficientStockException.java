package com.retailco.ecommerce.catalog;

public class InsufficientStockException extends RuntimeException {

    private final String productId;
    private final int requested;
    private final int available;

    public InsufficientStockException(String productId, int requested, int available) {
        super("insufficient stock for product " + productId + ": requested " + requested
                + " but only " + available + " available");
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public String getProductId() { return productId; }
    public int getRequested() { return requested; }
    public int getAvailable() { return available; }
}
