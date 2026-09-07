package com.retailco.ecommerce.cart;

// CONCEPT: Custom exception -- thrown when a cart line's quantity would
// exceed CartService.MAX_QUANTITY_PER_LINE. Applies to the RUNNING TOTAL,
// so adding 60 then 60 more of the same product fails on the second add,
// not just a single add of over 99.
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
