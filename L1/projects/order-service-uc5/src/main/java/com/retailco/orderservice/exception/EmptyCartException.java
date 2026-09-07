package com.retailco.orderservice.exception;

public class EmptyCartException extends RuntimeException {
    public EmptyCartException(String userId) {
        super("Cannot check out an empty cart for user: " + userId);
    }
}
