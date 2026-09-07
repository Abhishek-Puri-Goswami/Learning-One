package com.retailco.productservice.exception;

/**
 * A specific, named error we throw when someone asks for a product id
 * that doesn't exist. Giving it its own class (instead of just throwing a
 * generic error) means {@code GlobalExceptionHandler} can recognize it
 * and turn it into a proper "404 Not Found" response.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String productId) {
        super("Product not found: " + productId);
    }
}
