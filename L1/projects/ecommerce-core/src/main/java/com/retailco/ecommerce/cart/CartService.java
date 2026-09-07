package com.retailco.ecommerce.cart;

import com.retailco.ecommerce.catalog.ProductCatalog;
import com.retailco.ecommerce.model.Cart;
import com.retailco.ecommerce.model.CartItem;
import com.retailco.ecommerce.model.Product;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is the "service layer" for shopping carts — the place where the
 * actual business rules for adding, removing, and clearing cart items
 * live, separate from how carts are stored or shown to the user.
 * <p>
 * When you add an item, this class looks up its current price and name
 * from {@code ProductCatalog} and copies them into the cart line. It
 * deliberately does NOT check whether there's enough stock — that check
 * only happens later, at checkout. This matches how real shopping carts
 * behave: you can add more of something than is actually in stock, and
 * you only find out if that's a problem when you try to pay.
 * <p>
 * One detail worth understanding: {@code addItem} locks per user, not
 * globally. That means if two different customers are shopping at the
 * same time, they never block each other. But if the SAME customer sends
 * two "add this product" requests at almost the same moment (e.g. from a
 * double click), those two requests are handled one after another instead
 * of at the same time — otherwise we could lose one of the quantity
 * updates.
 */
public class CartService {

    /** A single cart line can never hold more than this many units of one product. */
    public static final int MAX_QUANTITY_PER_LINE = 99;

    private final Map<String, Cart> cartsByUserId = new ConcurrentHashMap<>();
    private final Map<String, Object> locksByUserId = new ConcurrentHashMap<>();
    private final ProductCatalog catalog;
    private final Clock clock;

    public CartService(ProductCatalog catalog, Clock clock) {
        this.catalog = catalog;
        this.clock = clock;
    }

    public Cart getOrCreateCart(String userId) {
        return cartsByUserId.computeIfAbsent(userId, id -> new Cart(id, Instant.now(clock)));
    }

    public Cart addItem(String userId, String productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        Product product = catalog.get(productId); // throws ProductNotFoundException if unknown

        // We give each user their own lock object. That way, two different
        // users adding items at the same time never wait on each other —
        // but if the SAME user's cart is being updated by two requests at
        // once, those two requests take turns instead of overlapping,
        // which is what keeps the quantity merge below accurate.
        Object lock = locksByUserId.computeIfAbsent(userId, id -> new Object());
        synchronized (lock) {
            Cart cart = getOrCreateCart(userId);
            int currentQuantity = cart.findByProductId(productId).map(CartItem::getQuantity).orElse(0);
            int runningTotal = currentQuantity + quantity;
            if (runningTotal > MAX_QUANTITY_PER_LINE) {
                throw new QuantityCapExceededException(productId, runningTotal, MAX_QUANTITY_PER_LINE);
            }
            CartItem item = new CartItem(UUID.randomUUID().toString(), product.getId(),
                    product.getName(), product.getPrice(), quantity);
            cart.addOrMergeItem(item, Instant.now(clock));
            return cart;
        }
    }

    public Cart removeItem(String userId, String productId) {
        Cart cart = getOrCreateCart(userId);
        cart.removeItem(productId, Instant.now(clock));
        return cart;
    }

    public Cart clearCart(String userId) {
        Cart cart = getOrCreateCart(userId);
        cart.clear(Instant.now(clock));
        return cart;
    }
}
