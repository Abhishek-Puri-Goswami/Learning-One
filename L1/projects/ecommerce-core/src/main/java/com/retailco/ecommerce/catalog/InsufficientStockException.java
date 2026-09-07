package com.retailco.ecommerce.catalog;

/**
 * Thrown when someone tries to buy more of a product than is currently in
 * stock. It carries the product id, how much was requested, and how much
 * is actually available, so whoever catches it can build a clear message
 * for the customer without having to pick apart the error text.
 */
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
