package com.retailco.ecommerce.order;

import com.retailco.ecommerce.catalog.InsufficientStockException;
import com.retailco.ecommerce.catalog.ProductCatalog;
import com.retailco.ecommerce.model.Cart;
import com.retailco.ecommerce.model.CartItem;
import com.retailco.ecommerce.model.Order;
import com.retailco.ecommerce.model.OrderLine;
import com.retailco.ecommerce.model.OrderStatus;
import com.retailco.ecommerce.payment.PaymentGateway;
import com.retailco.ecommerce.payment.PaymentResult;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The order-placement flow this submission's L1/UC4 refactor scenario and
 * L1/UC5 edge-case catalog exercise: reserve stock for every line, attempt
 * payment, and roll the reservation back line-by-line if payment fails --
 * so a declined card never leaves stock permanently short.
 *
 * <p>Reservation happens BEFORE payment (not after) deliberately: it holds
 * the item for this checkout attempt so two concurrent buyers of the last
 * unit can't both be told "payment succeeded." This is the same ordering
 * L1/UC4's refactored {@code order-service} documents in its AI review
 * report as a correction over the "before" version, which charged first
 * and only then checked stock.
 */
public class CheckoutService {

    private final ProductCatalog catalog;
    private final PaymentGateway paymentGateway;
    private final Clock clock;

    public CheckoutService(ProductCatalog catalog, PaymentGateway paymentGateway, Clock clock) {
        this.catalog = catalog;
        this.paymentGateway = paymentGateway;
        this.clock = clock;
    }

    /**
     * @throws IllegalArgumentException if the cart is empty
     * @throws InsufficientStockException if any line can't be reserved (nothing
     *         reserved so far for THIS call is left partially reserved -- see below)
     */
    public Order checkout(Cart cart) {
        if (cart.isEmpty()) {
            throw new IllegalArgumentException("cannot checkout an empty cart for user " + cart.getUserId());
        }

        List<CartItem> items = cart.getItems();
        List<String> reservedSoFar = new ArrayList<>();
        try {
            for (CartItem item : items) {
                catalog.reserveStock(item.getProductId(), item.getQuantity());
                reservedSoFar.add(item.getProductId());
            }
        } catch (InsufficientStockException e) {
            // Roll back every reservation this checkout attempt already made --
            // a partial reservation must never survive a failed checkout.
            for (String productId : reservedSoFar) {
                CartItem matching = items.stream()
                        .filter(i -> i.getProductId().equals(productId)).findFirst().orElseThrow();
                catalog.releaseStock(productId, matching.getQuantity());
            }
            throw e;
        }

        List<OrderLine> lines = items.stream().map(OrderLine::fromCartItem).toList();
        String orderId = "ORD-" + UUID.randomUUID();
        Order order = new Order(orderId, cart.getUserId(), lines, cart.getSubtotal(), Instant.now(clock));

        PaymentResult payment = paymentGateway.charge(orderId, cart.getUserId(), order.getTotal());
        if (payment.approved()) {
            order.setStatus(OrderStatus.PAID);
            cart.clear(Instant.now(clock));
        } else {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            // Payment failed after stock was reserved -- release every line back
            // to the catalog so a declined card never permanently shrinks stock.
            for (OrderLine line : lines) {
                catalog.releaseStock(line.productId(), line.quantity());
            }
        }
        return order;
    }
}
