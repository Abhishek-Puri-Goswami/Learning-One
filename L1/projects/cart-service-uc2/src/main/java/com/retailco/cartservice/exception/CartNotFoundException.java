package com.retailco.cartservice.exception;

// CONCEPT: Custom exception -- thrown when a cart doesn't exist for a
// given user.
public class CartNotFoundException extends RuntimeException {
    public CartNotFoundException(String userId) {
        super("Cart not found for user: " + userId);
    }
}
