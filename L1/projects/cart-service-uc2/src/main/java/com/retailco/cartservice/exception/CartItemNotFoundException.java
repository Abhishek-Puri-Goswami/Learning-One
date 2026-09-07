package com.retailco.cartservice.exception;

public class CartItemNotFoundException extends RuntimeException {
    public CartItemNotFoundException(String itemId) {
        super("Cart item not found: " + itemId);
    }
}
