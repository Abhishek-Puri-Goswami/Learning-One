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

// CONCEPT: Service layer -- orchestrates the checkout business process
// across several other classes (ProductCatalog, PaymentGateway).
// PURPOSE (see checkout() below, step by step):
// 1. Reserve stock for every line in the cart.
// 2. If any line can't be reserved, undo (release) whatever WAS already
//    reserved in this same attempt, then fail -- a checkout must never
//    leave a partial reservation behind.
// 3. Build the Order and attempt payment.
// 4. If payment succeeds: mark the order PAID and empty the cart.
//    If payment fails: mark it PAYMENT_FAILED and release the stock back
//    (a declined card must never permanently reduce stock).
// WHY reserve stock BEFORE charging payment (not after): if payment ran
// first, two people buying the last unit at the same time could both be
// charged successfully before either one's stock check ran. Reserving
// first means only one of them can actually get the stock -- the other
// fails immediately with "insufficient stock," before any money moves.
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
