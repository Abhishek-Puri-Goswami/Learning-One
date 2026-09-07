package com.retailco.cartservice.exception;

/** Thrown when there's no cart at all for the given user. */
public class CartNotFoundException extends RuntimeException {
    public CartNotFoundException(String userId) {
        super("Cart not found for user: " + userId);
    }
}
