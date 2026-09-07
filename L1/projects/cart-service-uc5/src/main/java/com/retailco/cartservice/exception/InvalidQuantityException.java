package com.retailco.cartservice.exception;

// CONCEPT: Custom exception -- thrown when a requested quantity exceeds
// the allowed maximum. Checked again in the service layer (not only via a
// DTO validation annotation) so any caller of CartService is protected,
// not only ones going through the REST controller.
public class InvalidQuantityException extends RuntimeException {
    public InvalidQuantityException(int requestedQuantity, int max) {
        super("Requested quantity " + requestedQuantity + " exceeds the maximum of " + max + " per line item");
    }
}
