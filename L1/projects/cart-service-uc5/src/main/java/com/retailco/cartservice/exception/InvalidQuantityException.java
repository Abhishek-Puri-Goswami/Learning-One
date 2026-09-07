package com.retailco.cartservice.exception;

/**
 * Thrown when a requested quantity is more than the allowed maximum. This
 * check runs again inside the service layer, not only through the
 * {@code @Max} validation on the request DTO — that way, even a caller
 * that skips the REST controller and calls {@code CartService} directly
 * is still protected by the same rule.
 */
public class InvalidQuantityException extends RuntimeException {
    public InvalidQuantityException(int requestedQuantity, int max) {
        super("Requested quantity " + requestedQuantity + " exceeds the maximum of " + max + " per line item");
    }
}
