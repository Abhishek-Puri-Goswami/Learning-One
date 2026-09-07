package com.retailco.ecommerce.cart;

/**
 * Mirrors L1/UC5's edge-case catalog: {@code largeQuantity_overCap_throwsInvalidQuantityException}
 * and {@code largeQuantity_sumAcrossTwoAdds_overCap_throwsOnSecondAdd}. The
 * cap applies to a line's running total, not just a single request -- adding
 * 60 then 60 more of the same product must reject the second call, not
 * silently accept 120.
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
