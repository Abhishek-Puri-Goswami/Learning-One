package com.retailco.ecommerce.cart;

/**
 * Thrown when adding items to a cart would push one product's quantity
 * past the allowed limit ({@code CartService.MAX_QUANTITY_PER_LINE}).
 * This check looks at the running total, not just the current request —
 * so if you add 60 of something and then try to add 60 more, the second
 * request fails, even though neither request alone was over the limit.
 */
public class QuantityCapExceededException extends RuntimeException {

    private final String productId;
    private final int requestedRunningTotal;
    private final int cap;

    public QuantityCapExceededException(String productId, int requestedRunningTotal, int cap) {
        super("quantity cap exceeded for product " + productId + ": requested running total "
                + requestedRunningTotal + " exceeds cap of " + cap);
        this.productId = productId;
        this.requestedRunningTotal = requestedRunningTotal;
        this.cap = cap;
    }

    public String getProductId() { return productId; }
    public int getRequestedRunningTotal() { return requestedRunningTotal; }
    public int getCap() { return cap; }
}
