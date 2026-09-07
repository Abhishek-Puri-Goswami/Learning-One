package com.retailco.orderservice.exception;

/**
 * Thrown when checkout would sell more of a product than we actually have
 * in stock. Since the cart only ever holds a snapshot (not a live check),
 * this exception is what catches the case where stock has run out between
 * when someone added an item to their cart and when they tried to pay for
 * it.
 */
public class OutOfStockException extends RuntimeException {
    public OutOfStockException(String productId, int requested, int available) {
        super("Product " + productId + " is out of stock: requested " + requested
                + ", only " + available + " available");
    }
}
