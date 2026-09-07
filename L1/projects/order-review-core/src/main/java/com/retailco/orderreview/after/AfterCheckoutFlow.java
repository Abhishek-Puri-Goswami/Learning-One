package com.retailco.orderreview.after;

/**
 * Pure-JDK mirror of the FIX for AI-QA-1: in
 * {@code order-service-refactored}, {@code shippingAddress} is guaranteed
 * non-null by {@code @Valid @NotNull} on {@code OrderRequest} (Bean
 * Validation runs before the service layer is ever invoked), so
 * {@code OrderServiceImpl} needs no defensive null check of its own. This
 * class reproduces that guarantee explicitly (validate-then-resolve) so the
 * "guaranteed non-null" claim is something a test can actually exercise,
 * not just assert.
 */
public final class AfterCheckoutFlow {

    private AfterCheckoutFlow() {
    }

    public static String resolveCity(ShippingAddressLike shippingAddress) {
        if (shippingAddress == null) {
            // In the real service this branch is unreachable in practice
            // (Bean Validation rejects the request first) -- but resolveCity
            // itself no longer trusts that alone: it fails with a clear,
            // named validation error instead of an opaque NPE either way.
            throw new IllegalArgumentException("shippingAddress must not be null");
        }
        return shippingAddress.getCity();
    }

    public interface ShippingAddressLike {
        String getCity();
    }
}
