package com.retailco.ecommerce.selftest;

import com.retailco.ecommerce.catalog.InsufficientStockException;
import com.retailco.ecommerce.catalog.ProductCatalog;
import com.retailco.ecommerce.catalog.ProductNotFoundException;
import com.retailco.ecommerce.cart.CartService;
import com.retailco.ecommerce.cart.QuantityCapExceededException;
import com.retailco.ecommerce.model.Cart;
import com.retailco.ecommerce.model.Order;
import com.retailco.ecommerce.model.OrderStatus;
import com.retailco.ecommerce.model.Product;
import com.retailco.ecommerce.order.CheckoutService;
import com.retailco.ecommerce.payment.PaymentGateway;
import com.retailco.ecommerce.payment.StubPaymentGateway;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hand-rolled self-test harness (no JUnit -- Maven Central is blocked, same
 * constraint documented throughout this submission). Mirrors the
 * [PASS]/[FAIL] console-report pattern used by L2's {@code SelfTests} and
 * {@code RbacAndAuditSelfTests}. Run with:
 *   java -cp out com.retailco.ecommerce.selftest.SelfTests
 * Exits non-zero if any test fails, so it can gate a CI job the same way
 * L2/UC6's pipeline design does.
 */
public final class SelfTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-31T00:00:00Z"), ZoneOffset.UTC);
    private static final AtomicInteger PASS = new AtomicInteger();
    private static final AtomicInteger FAIL = new AtomicInteger();

    public static void main(String[] args) {
        testAddItemToCartSnapshotsCurrentPrice();
        testAddingSameProductTwiceMergesQuantity();
        testCheckoutHappyPathReservesStockAndClearsCart();
        testCheckoutOversellIsRejectedAndStockUnchanged();
        testCheckoutPartialReservationRollsBackOnLaterLineFailure();
        testCheckoutDeclinedPaymentReleasesReservedStock();
        testCheckoutEmptyCartRejected();
        testUnknownProductLookupThrows();
        testConcurrentCheckoutsCannotOversellLastUnit();
        testRestockIncreasesAvailability();
        testRemoveItemByIdRemovesOnlyThatLine();
        testSetItemQuantityByIdUpdatesOnlyThatLine();
        testQuantityAtCapIsAccepted();
        testQuantityOverCapIsRejected();
        testQuantitySumAcrossTwoAddsOverCapRejectedOnSecondAdd();
        testConcurrentAddsOfSameProductMergeWithNoLostUpdates();
        testCheckoutStockExactlyMatchesRequestedQuantityBoundaryAccepted();

        System.out.println();
        System.out.println("Results: " + PASS.get() + " passed, " + FAIL.get() + " failed");
        if (FAIL.get() > 0) {
            System.exit(1);
        }
    }

    private static ProductCatalog freshCatalog() {
        ProductCatalog catalog = new ProductCatalog(CLOCK);
        catalog.put(new Product("P-100", "Wireless Mouse", "Ergonomic 2.4GHz mouse",
                new BigDecimal("25.00"), "Electronics", 10, Instant.now(CLOCK), Instant.now(CLOCK)));
        catalog.put(new Product("P-200", "Mechanical Keyboard", "Hot-swappable, brown switches",
                new BigDecimal("89.99"), "Electronics", 3, Instant.now(CLOCK), Instant.now(CLOCK)));
        catalog.put(new Product("P-300", "4K Monitor", "27-inch IPS panel",
                new BigDecimal("5000.00"), "Electronics", 5, Instant.now(CLOCK), Instant.now(CLOCK)));
        return catalog;
    }

    private static void testAddItemToCartSnapshotsCurrentPrice() {
        String name = "addItem snapshots product name/price at add-time";
        try {
            ProductCatalog catalog = freshCatalog();
            CartService cartService = new CartService(catalog, CLOCK);
            Cart cart = cartService.addItem("U1", "P-100", 2);
            check(name, cart.getItems().size() == 1
                    && cart.getItems().get(0).getUnitPrice().equals(new BigDecimal("25.00"))
                    && cart.getSubtotal().equals(new BigDecimal("50.00")));
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testAddingSameProductTwiceMergesQuantity() {
        String name = "adding the same product twice merges quantity, not a duplicate line";
        try {
            ProductCatalog catalog = freshCatalog();
            CartService cartService = new CartService(catalog, CLOCK);
            cartService.addItem("U2", "P-100", 1);
            Cart cart = cartService.addItem("U2", "P-100", 2);
            check(name, cart.getItems().size() == 1 && cart.getItems().get(0).getQuantity() == 3);
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testCheckoutHappyPathReservesStockAndClearsCart() {
        String name = "checkout: happy path reserves stock, charges, clears cart";
        try {
            ProductCatalog catalog = freshCatalog();
            CartService cartService = new CartService(catalog, CLOCK);
            CheckoutService checkoutService = new CheckoutService(catalog, new StubPaymentGateway(), CLOCK);

            Cart cart = cartService.addItem("U3", "P-100", 2);
            Order order = checkoutService.checkout(cart);

            check(name, order.getStatus() == OrderStatus.PAID
                    && order.getTotal().equals(new BigDecimal("50.00"))
                    && catalog.get("P-100").getStockQuantity() == 8
                    && cart.isEmpty());
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testCheckoutOversellIsRejectedAndStockUnchanged() {
        String name = "checkout: requesting more than available stock throws and leaves stock untouched";
        try {
            ProductCatalog catalog = freshCatalog();
            CartService cartService = new CartService(catalog, CLOCK);
            CheckoutService checkoutService = new CheckoutService(catalog, new StubPaymentGateway(), CLOCK);

            Cart cart = cartService.addItem("U4", "P-200", 5); // only 3 in stock
            boolean threw = false;
            try {
                checkoutService.checkout(cart);
            } catch (InsufficientStockException e) {
                threw = true;
            }
            check(name, threw && catalog.get("P-200").getStockQuantity() == 3);
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testCheckoutPartialReservationRollsBackOnLaterLineFailure() {
        String name = "checkout: a multi-line cart's earlier successful reservation is rolled back if a later line fails";
        try {
            ProductCatalog catalog = freshCatalog();
            CartService cartService = new CartService(catalog, CLOCK);
            CheckoutService checkoutService = new CheckoutService(catalog, new StubPaymentGateway(), CLOCK);

            Cart cart = cartService.addItem("U5", "P-100", 2);  // succeeds (10 available)
            cartService.addItem("U5", "P-200", 10);              // fails (only 3 available)

            boolean threw = false;
            try {
                checkoutService.checkout(cart);
            } catch (InsufficientStockException e) {
                threw = true;
            }
            // P-100's reservation from this same failed checkout attempt must be rolled back to 10.
            check(name, threw && catalog.get("P-100").getStockQuantity() == 10
                    && catalog.get("P-200").getStockQuantity() == 3);
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testCheckoutDeclinedPaymentReleasesReservedStock() {
        String name = "checkout: a declined payment releases the stock it had reserved";
        try {
            ProductCatalog catalog = freshCatalog();
            CartService cartService = new CartService(catalog, CLOCK);
            CheckoutService checkoutService = new CheckoutService(catalog, new StubPaymentGateway(), CLOCK);

            Cart cart = cartService.addItem(StubPaymentGateway.FORCED_DECLINE_USER_ID, "P-100", 4);
            Order order = checkoutService.checkout(cart);

            check(name, order.getStatus() == OrderStatus.PAYMENT_FAILED
                    && catalog.get("P-100").getStockQuantity() == 10 // released back to original
                    && !cart.isEmpty()); // failed order does not clear the cart
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testCheckoutEmptyCartRejected() {
        String name = "checkout: an empty cart is rejected before touching the catalog or payment gateway";
        try {
            ProductCatalog catalog = freshCatalog();
            CartService cartService = new CartService(catalog, CLOCK);
            CheckoutService checkoutService = new CheckoutService(catalog, new StubPaymentGateway(), CLOCK);

            Cart cart = cartService.getOrCreateCart("U6");
            boolean threw = false;
            try {
                checkoutService.checkout(cart);
            } catch (IllegalArgumentException e) {
                threw = true;
            }
            check(name, threw);
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testUnknownProductLookupThrows() {
        String name = "adding an unknown productId to a cart throws ProductNotFoundException";
        try {
            ProductCatalog catalog = freshCatalog();
            CartService cartService = new CartService(catalog, CLOCK);
            boolean threw = false;
            try {
                cartService.addItem("U7", "P-DOES-NOT-EXIST", 1);
            } catch (ProductNotFoundException e) {
                threw = true;
            }
            check(name, threw);
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testConcurrentCheckoutsCannotOversellLastUnit() {
        String name = "concurrent checkouts for the last unit: exactly one buyer wins, stock never goes negative";
        try {
            ProductCatalog catalog = freshCatalog();
            catalog.put(new Product("P-LAST", "Limited Edition Widget", "Only one left",
                    new BigDecimal("10.00"), "Electronics", 1, Instant.now(CLOCK), Instant.now(CLOCK)));
            PaymentGateway gateway = new StubPaymentGateway();
            CheckoutService checkoutService = new CheckoutService(catalog, gateway, CLOCK);

            int threadCount = 8;
            AtomicInteger successes = new AtomicInteger();
            AtomicInteger stockFailures = new AtomicInteger();
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                final String userId = "CONCURRENT-U" + i;
                threads[i] = new Thread(() -> {
                    CartService cartService = new CartService(catalog, CLOCK);
                    Cart cart = cartService.addItem(userId, "P-LAST", 1);
                    try {
                        Order order = checkoutService.checkout(cart);
                        if (order.getStatus() == OrderStatus.PAID) {
                            successes.incrementAndGet();
                        }
                    } catch (InsufficientStockException e) {
                        stockFailures.incrementAndGet();
                    }
                });
            }
            for (Thread t : threads) t.start();
            for (Thread t : threads) t.join();

            check(name, successes.get() == 1 && stockFailures.get() == threadCount - 1
                    && catalog.get("P-LAST").getStockQuantity() == 0);
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testRestockIncreasesAvailability() {
        String name = "restock increases available stock, permitting a checkout that previously would have failed";
        try {
            ProductCatalog catalog = freshCatalog();
            CartService cartService = new CartService(catalog, CLOCK);
            CheckoutService checkoutService = new CheckoutService(catalog, new StubPaymentGateway(), CLOCK);

            catalog.restock("P-200", 5); // 3 -> 8
            Cart cart = cartService.addItem("U8", "P-200", 5);
            Order order = checkoutService.checkout(cart);

            check(name, order.getStatus() == OrderStatus.PAID && catalog.get("P-200").getStockQuantity() == 3);
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testRemoveItemByIdRemovesOnlyThatLine() {
        String name = "Cart.removeItemById removes only the targeted line, by itemId not productId";
        try {
            ProductCatalog catalog = freshCatalog();
            CartService cartService = new CartService(catalog, CLOCK);
            Cart cart = cartService.addItem("U9", "P-100", 1);
            cart.addOrMergeItem(new com.retailco.ecommerce.model.CartItem(
                    "manual-item-1", "P-200", "Mechanical Keyboard", new BigDecimal("89.99"), 1), Instant.now(CLOCK));
            String targetItemId = cart.findByProductId("P-100").orElseThrow().getItemId();

            boolean removed = cart.removeItemById(targetItemId, Instant.now(CLOCK));

            check(name, removed && cart.getItems().size() == 1
                    && cart.getItems().get(0).getProductId().equals("P-200"));
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testSetItemQuantityByIdUpdatesOnlyThatLine() {
        String name = "Cart.setItemQuantity updates only the targeted line's quantity";
        try {
            ProductCatalog catalog = freshCatalog();
            CartService cartService = new CartService(catalog, CLOCK);
            Cart cart = cartService.addItem("U10", "P-100", 1);
            String targetItemId = cart.findByProductId("P-100").orElseThrow().getItemId();

            cart.setItemQuantity(targetItemId, 5, Instant.now(CLOCK));

            check(name, cart.getItems().size() == 1 && cart.getItems().get(0).getQuantity() == 5
                    && cart.getSubtotal().equals(new BigDecimal("125.00")));
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testQuantityAtCapIsAccepted() {
        String name = "cart quantity cap: exactly 99 units in one add is accepted (inclusive boundary)";
        try {
            ProductCatalog catalog = freshCatalog();
            catalog.put(new Product("P-BULK", "Bulk Widget", "high-stock item",
                    new BigDecimal("1.00"), "Misc", 1000, Instant.now(CLOCK), Instant.now(CLOCK)));
            CartService cartService = new CartService(catalog, CLOCK);
            Cart cart = cartService.addItem("U11", "P-BULK", 99);
            check(name, cart.findByProductId("P-BULK").orElseThrow().getQuantity() == 99);
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testQuantityOverCapIsRejected() {
        String name = "cart quantity cap: 999,999 units in one add is rejected, not truncated or accepted";
        try {
            ProductCatalog catalog = freshCatalog();
            catalog.put(new Product("P-BULK2", "Bulk Widget 2", "high-stock item",
                    new BigDecimal("1.00"), "Misc", 2_000_000, Instant.now(CLOCK), Instant.now(CLOCK)));
            CartService cartService = new CartService(catalog, CLOCK);
            boolean threw = false;
            try {
                cartService.addItem("U12", "P-BULK2", 999_999);
            } catch (QuantityCapExceededException e) {
                threw = true;
            }
            check(name, threw);
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testQuantitySumAcrossTwoAddsOverCapRejectedOnSecondAdd() {
        String name = "cart quantity cap: 60 + 60 of the same product (sum 120 > cap 99) rejects the second add";
        try {
            ProductCatalog catalog = freshCatalog();
            catalog.put(new Product("P-BULK3", "Bulk Widget 3", "high-stock item",
                    new BigDecimal("1.00"), "Misc", 1000, Instant.now(CLOCK), Instant.now(CLOCK)));
            CartService cartService = new CartService(catalog, CLOCK);
            cartService.addItem("U13", "P-BULK3", 60); // first add succeeds
            boolean threw = false;
            try {
                cartService.addItem("U13", "P-BULK3", 60); // running total 120 > 99
            } catch (QuantityCapExceededException e) {
                threw = true;
            }
            Cart cart = cartService.getOrCreateCart("U13");
            check(name, threw && cart.findByProductId("P-BULK3").orElseThrow().getQuantity() == 60);
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testConcurrentAddsOfSameProductMergeWithNoLostUpdates() {
        String name = "20 threads concurrently adding 1x the same product merge into one line with quantity 20, no lost updates";
        try {
            ProductCatalog catalog = freshCatalog();
            catalog.put(new Product("P-CONC", "Concurrent Widget", "stress-test item",
                    new BigDecimal("5.00"), "Misc", 1000, Instant.now(CLOCK), Instant.now(CLOCK)));
            CartService cartService = new CartService(catalog, CLOCK);

            int threadCount = 20;
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> cartService.addItem("U14", "P-CONC", 1));
            }
            for (Thread t : threads) t.start();
            for (Thread t : threads) t.join();

            Cart cart = cartService.getOrCreateCart("U14");
            check(name, cart.getItems().size() == 1
                    && cart.getItems().get(0).getQuantity() == threadCount);
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void testCheckoutStockExactlyMatchesRequestedQuantityBoundaryAccepted() {
        String name = "checkout: requested quantity exactly equals available stock (boundary) is accepted -- the last-unit purchase succeeds";
        try {
            ProductCatalog catalog = freshCatalog(); // P-200 has exactly 3 in stock
            CartService cartService = new CartService(catalog, CLOCK);
            CheckoutService checkoutService = new CheckoutService(catalog, new StubPaymentGateway(), CLOCK);

            Cart cart = cartService.addItem("U15", "P-200", 3); // exactly matches available stock
            Order order = checkoutService.checkout(cart);

            check(name, order.getStatus() == OrderStatus.PAID && catalog.get("P-200").getStockQuantity() == 0);
        } catch (Exception e) {
            fail(name, e);
        }
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            PASS.incrementAndGet();
            System.out.println("[PASS] " + name);
        } else {
            FAIL.incrementAndGet();
            System.out.println("[FAIL] " + name + " -- condition was false");
        }
    }

    private static void fail(String name, Exception e) {
        FAIL.incrementAndGet();
        System.out.println("[FAIL] " + name + " -- threw " + e.getClass().getSimpleName() + ": " + e.getMessage());
    }
}
