package com.retailco.ecommerce.catalog;

/** Thrown when someone asks for a product id that doesn't exist in the catalog. */
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String productId) {
        super("no product with id " + productId);
    }
}
