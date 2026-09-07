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
 * This class runs the whole checkout process, step by step, coordinating
 * between the product catalog and the payment gateway. Here's what
 * happens, in order, when {@link #checkout} is called:
 * <ol>
 *   <li>Try to reserve (set aside) enough stock for every item in the cart.</li>
 *   <li>If any item can't be reserved (not enough stock), undo any
 *       reservations already made in this same attempt and stop — we
 *       never want to leave stock "half reserved."</li>
 *   <li>Build the order and attempt to charge the customer.</li>
 *   <li>If payment succeeds, mark the order as PAID and empty the cart.
 *       If payment fails, mark it PAYMENT_FAILED and give the reserved
 *       stock back — a declined card should never permanently shrink our
 *       stock count.</li>
 * </ol>
 * <p>
 * One important design choice: we reserve stock <b>before</b> charging
 * the card, not after. Imagine two customers trying to buy the very last
 * item at the same moment. If we charged their cards first and checked
 * stock second, both charges could succeed before either stock check
 * runs — and now we've sold one item to two people. By reserving stock
 * first, only one of them can actually claim it; the other finds out
 * immediately ("not enough stock") before any money changes hands.
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
            // Something couldn't be reserved. Give back everything we DID
            // manage to reserve in this attempt, so we don't leave stock
            // stuck in limbo after a failed checkout.
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
            // The card was declined, but we'd already reserved the stock
            // for this order. Give it all back so a failed payment doesn't
            // permanently shrink how much stock we have available.
            for (OrderLine line : lines) {
                catalog.releaseStock(line.productId(), line.quantity());
            }
        }
        return order;
    }
}
