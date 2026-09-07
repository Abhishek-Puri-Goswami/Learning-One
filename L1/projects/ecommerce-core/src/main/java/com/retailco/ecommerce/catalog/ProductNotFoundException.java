package com.retailco.ecommerce.catalog;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String productId) {
        super("no product with id " + productId);
    }
}
