package com.retailco.orderreview.before;

// CONCEPT: Minimal, isolated reproduction of a real bug (missing null
// check) so it can be demonstrated and tested on its own, without needing
// the whole Spring Boot app running.
/**
 * Reproduces AI review finding AI-QA-1: {@code order-service-before}'s
 * {@code OrderServiceImpl.checkout()} calls
 * {@code request.getShippingAddress().getCity()} with no null check. If
 * {@code shippingAddress} is null (a request that omits it -- there is no
 * Bean Validation on {@code OrderRequest} in the "before" version either,
 * see AI-QA-2), this throws an unhandled {@link NullPointerException}.
 */
public final class BeforeCheckoutFlow {

    private BeforeCheckoutFlow() {
    }

    /** @param shippingAddress deliberately nullable -- reproduces the unguarded request shape. */
    public static String resolveCity(ShippingAddressLike shippingAddress) {
        return shippingAddress.getCity(); // NPE here if shippingAddress is null -- this IS the bug.
    }

    public interface ShippingAddressLike {
        String getCity();
    }
}
