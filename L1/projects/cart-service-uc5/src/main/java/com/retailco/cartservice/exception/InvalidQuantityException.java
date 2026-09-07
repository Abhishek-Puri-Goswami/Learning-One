package com.retailco.cartservice.exception;

/**
 * L1/UC5 defense-in-depth: enforced again at the service layer (not just via
 * @Max on the DTO) so any internal/non-HTTP caller of CartService is also
 * protected. See edge-cases/edge-case-catalog.md, "Large quantity".
 */
public class InvalidQuantityException extends RuntimeException {
    public InvalidQuantityException(int requestedQuantity, int max) {
        super("Requested quantity " + requestedQuantity + " exceeds the maximum of " + max + " per line item");
    }
}
