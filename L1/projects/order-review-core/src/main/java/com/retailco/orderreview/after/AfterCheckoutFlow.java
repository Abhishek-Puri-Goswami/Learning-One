package com.retailco.orderreview.after;

/**
 * The fixed version of {@link com.retailco.orderreview.before.BeforeCheckoutFlow}.
 * In the real, fixed order-service, {@code shippingAddress} can never
 * actually be null by the time it reaches this logic — Spring's
 * {@code @Valid}/{@code @NotNull} validation on the request already
 * rejects a request missing it, before our own code ever runs.
 * <p>
 * Still, this method checks for null anyway and throws a clear,
 * descriptive error if it somehow got one. That's a good habit called
 * "defense in depth": even when you're confident another layer already
 * protects you, a cheap extra check here means this method never produces
 * a confusing crash, no matter what calls it in the future.
 */
public final class AfterCheckoutFlow {

    private AfterCheckoutFlow() {
    }

    public static String resolveCity(ShippingAddressLike shippingAddress) {
        if (shippingAddress == null) {
            // In practice this should never happen — validation catches a
            // missing shipping address earlier — but if it ever does, we'd
            // rather fail with a clear, specific message than an opaque
            // NullPointerException.
            throw new IllegalArgumentException("shippingAddress must not be null");
        }
        return shippingAddress.getCity();
    }

    public interface ShippingAddressLike {
        String getCity();
    }
}
