package com.retailco.ecommerce.demo;

import com.retailco.ecommerce.catalog.InsufficientStockException;
import com.retailco.ecommerce.catalog.ProductCatalog;
import com.retailco.ecommerce.cart.CartService;
import com.retailco.ecommerce.model.Cart;
import com.retailco.ecommerce.model.Order;
import com.retailco.ecommerce.model.Product;
import com.retailco.ecommerce.order.CheckoutService;
import com.retailco.ecommerce.payment.StubPaymentGateway;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

/**
 * Narrative end-to-end demo (mirrors L2/L3's {@code Main} pattern): seed a
 * catalog, add to cart, checkout a happy path, then checkout a path that
 * hits insufficient stock. Run with:
 *   java -cp out com.retailco.ecommerce.demo.Main
 */
public final class Main {

    public static void main(String[] args) {
        Clock clock = Clock.systemUTC();
        ProductCatalog catalog = new ProductCatalog(clock);
        catalog.put(new Product("P-100", "Wireless Mouse", "Ergonomic 2.4GHz mouse",
                new BigDecimal("25.00"), "Electronics", 10, Instant.now(clock), Instant.now(clock)));
        catalog.put(new Product("P-200", "Mechanical Keyboard", "Hot-swappable, brown switches",
                new BigDecimal("89.99"), "Electronics", 3, Instant.now(clock), Instant.now(clock)));

        CartService cartService = new CartService(catalog, clock);
        CheckoutService checkoutService = new CheckoutService(catalog, new StubPaymentGateway(), clock);

        System.out.println("=== ecommerce-core demo ===");
        System.out.println("Initial catalog:");
        catalog.findAll().forEach(p -> System.out.println("  " + p));

        System.out.println();
        System.out.println("Turn 1: CUST-1 adds 2x Wireless Mouse, checks out");
        Cart cart1 = cartService.addItem("CUST-1", "P-100", 2);
        System.out.println("  cart before checkout: " + cart1);
        Order order1 = checkoutService.checkout(cart1);
        System.out.println("  order: " + order1);
        System.out.println("  P-100 stock after: " + catalog.get("P-100").getStockQuantity());

        System.out.println();
        System.out.println("Turn 2: CUST-2 tries to buy 5x Mechanical Keyboard (only 3 in stock)");
        Cart cart2 = cartService.addItem("CUST-2", "P-200", 5);
        try {
            checkoutService.checkout(cart2);
            System.out.println("  UNEXPECTED: checkout succeeded");
        } catch (InsufficientStockException e) {
            System.out.println("  [EXPECTED] checkout rejected: " + e.getMessage());
        }
        System.out.println("  P-200 stock after (unchanged): " + catalog.get("P-200").getStockQuantity());

        System.out.println();
        System.out.println("Turn 3: CUST-3's card is declined (forced-decline sentinel user)");
        Cart cart3 = cartService.addItem(StubPaymentGateway.FORCED_DECLINE_USER_ID, "P-100", 3);
        Order order3 = checkoutService.checkout(cart3);
        System.out.println("  order: " + order3);
        System.out.println("  P-100 stock after (released back): " + catalog.get("P-100").getStockQuantity());

        System.out.println();
        System.out.println("=== demo complete ===");
    }
}
