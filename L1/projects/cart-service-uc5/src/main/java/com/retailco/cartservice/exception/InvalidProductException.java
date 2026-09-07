package com.retailco.cartservice.exception;

/**
 * Thrown when a product id truly doesn't exist in the catalog (confirmed
 * by a real 404 response) — as opposed to a temporary network problem,
 * which is handled differently (see {@link com.retailco.cartservice.client.ProductCatalogClient}).
 * Keeping these two situations separate matters: an invalid product
 * should be rejected outright, while a temporary outage should just be
 * shrugged off gracefully.
 */
public class InvalidProductException extends RuntimeException {
    public InvalidProductException(String productId) {
        super("Product does not exist in the catalog: " + productId);
    }
}
