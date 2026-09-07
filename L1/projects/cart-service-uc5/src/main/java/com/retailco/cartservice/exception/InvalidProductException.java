package com.retailco.cartservice.exception;

// CONCEPT: Custom exception -- thrown when a productId truly doesn't
// exist in the catalog (a confirmed 404), as opposed to a temporary
// network problem. Keeping these two cases separate matters: an invalid
// product should be rejected, while a temporary outage should just
// degrade gracefully (see ProductCatalogClient).
public class InvalidProductException extends RuntimeException {
    public InvalidProductException(String productId) {
        super("Product does not exist in the catalog: " + productId);
    }
}
