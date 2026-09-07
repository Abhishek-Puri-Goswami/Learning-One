package com.retailco.productservice.exception;

// CONCEPT: Custom exception -- a specific, named error type instead of a
// generic RuntimeException.
// PURPOSE: Thrown when a product id doesn't exist. GlobalExceptionHandler
// catches this exact type and turns it into a 404 response.
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String productId) {
        super("Product not found: " + productId);
    }
}
