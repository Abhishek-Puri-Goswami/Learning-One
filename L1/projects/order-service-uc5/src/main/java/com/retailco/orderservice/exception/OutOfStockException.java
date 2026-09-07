package com.retailco.orderservice.exception;

/**
 * L1/UC5 addition (see edge-cases/edge-case-catalog.md, "Out-of-stock"):
 * order-service did not previously re-check stock at all before charging
 * the customer -- Cart is explicitly non-authoritative for stock (see L1/UC1
 * architecture.json), but nothing in checkout() ever re-validated it against
 * the Catalog, so an out-of-stock item could be charged and "confirmed."
 */
public class OutOfStockException extends RuntimeException {
    public OutOfStockException(String productId, int requested, int available) {
        super("Product " + productId + " is out of stock: requested " + requested
                + ", only " + available + " available");
    }
}
