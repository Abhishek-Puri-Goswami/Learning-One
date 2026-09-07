package com.retailco.orderservice.exception;

// CONCEPT: Custom exception -- thrown when trying to checkout with no
// items in the cart.
public class EmptyCartException extends RuntimeException {
    public EmptyCartException(String userId) {
        super("Cannot check out an empty cart for user: " + userId);
    }
}
