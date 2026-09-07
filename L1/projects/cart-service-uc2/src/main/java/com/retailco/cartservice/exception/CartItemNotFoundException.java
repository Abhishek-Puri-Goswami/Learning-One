package com.retailco.cartservice.exception;

/**
 * Thrown when someone tries to update or remove a cart line that doesn't
 * exist anymore (or never did) — for example, trying to delete an item
 * that's already been removed.
 */
public class CartItemNotFoundException extends RuntimeException {
    public CartItemNotFoundException(String itemId) {
        super("Cart item not found: " + itemId);
    }
}
