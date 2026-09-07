package com.retailco.ecommerce.catalog;

// CONCEPT: Custom exception carrying extra data (productId, requested,
// available) so a caller can build a helpful error message without
// re-parsing the exception's text.
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
