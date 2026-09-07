package com.retailco.cartservice.exception;

// CONCEPT: Custom exception -- thrown when a specific cart line (by
// itemId) doesn't exist, e.g. trying to update/remove a line that's
// already gone.
public class CartItemNotFoundException extends RuntimeException {
    public CartItemNotFoundException(String itemId) {
        super("Cart item not found: " + itemId);
    }
}
