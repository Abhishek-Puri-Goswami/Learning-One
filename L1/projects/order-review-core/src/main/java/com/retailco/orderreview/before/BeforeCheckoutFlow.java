package com.retailco.orderreview.before;

/**
 * A tiny, standalone example showing a real bug from
 * {@code order-service-before}'s checkout logic: calling
 * {@code shippingAddress.getCity()} without first checking whether
 * {@code shippingAddress} is null. If a request comes in without a
 * shipping address, this crashes with an ugly, unhelpful
 * {@link NullPointerException} instead of a clear error message.
 * <p>
 * This class is kept separate from the full Spring Boot app on purpose,
 * so this one specific bug can be demonstrated and tested in isolation,
 * without needing the whole application running.
 */
public final class BeforeCheckoutFlow {

    private BeforeCheckoutFlow() {
    }

    /** @param shippingAddress can be null here on purpose, to reproduce the bug. */
    public static String resolveCity(ShippingAddressLike shippingAddress) {
        return shippingAddress.getCity(); // Crashes right here if shippingAddress is null — that's the bug.
    }

    public interface ShippingAddressLike {
        String getCity();
    }
}
