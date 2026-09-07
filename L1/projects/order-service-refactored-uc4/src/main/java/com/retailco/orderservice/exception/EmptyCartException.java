package com.retailco.orderservice.exception;

/** Thrown when someone tries to check out with an empty cart. */
public class EmptyCartException extends RuntimeException {
    public EmptyCartException(String userId) {
        super("Cannot check out an empty cart for user: " + userId);
    }
}
