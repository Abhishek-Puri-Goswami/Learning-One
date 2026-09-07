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
 * Stands in for L1/UC2's {@code cart-service} controller/service layer.
 * Reads current price/name from {@link ProductCatalog} at add-time (a
 * snapshot, per {@link Cart}'s documented contract) but does NOT reserve
 * stock -- stock is only reserved at checkout, by
 * {@link com.retailco.ecommerce.order.CheckoutService}, matching how a real
 * cart lets you add more of an item than is currently in stock (you find
 * out at checkout, not at add-to-cart).
 *
 * <p>Two rules added for L1/UC5's edge-case catalog: a per-line quantity
 * cap ({@link #MAX_QUANTITY_PER_LINE}) that applies to the *running total*
 * across repeated adds of the same product, not just a single request; and
 * {@code addItem} is synchronized per-user so many threads adding the same
 * product concurrently merge into one line with no lost updates, rather
 * than racing on the cart's item list.
 */
public class CartService {

    /** Mirrors L1/UC5's edge-case catalog: 99 is the inclusive boundary. */
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

        // One lock object per userId so concurrent adds to DIFFERENT users'
        // carts never contend, but concurrent adds to the SAME user's cart
        // (e.g. the same product added by 20 threads at once) are strictly
        // serialized -- this is what makes the merge in Cart.addOrMergeItem
        // race-free instead of losing updates.
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
