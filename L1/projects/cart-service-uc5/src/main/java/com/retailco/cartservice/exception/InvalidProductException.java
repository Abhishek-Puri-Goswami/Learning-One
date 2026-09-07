package com.retailco.cartservice.exception;

/**
 * L1/UC5 fix: previously ProductCatalogClient silently fell back to a
 * placeholder ("Unknown product", price 0) for ANY failure, including a
 * genuine 404 (product truly does not exist). That meant an invalid
 * productId could be added to a cart at price 0 instead of being rejected --
 * see edge-cases/edge-case-catalog.md, "Invalid product ID". This exception
 * is now thrown specifically for a confirmed 404 and propagated as a 404 to
 * the caller; a transient/network failure still degrades gracefully (see
 * ProductCatalogClient.fetchProduct).
 */
public class InvalidProductException extends RuntimeException {
    public InvalidProductException(String productId) {
        super("Product does not exist in the catalog: " + productId);
    }
}
