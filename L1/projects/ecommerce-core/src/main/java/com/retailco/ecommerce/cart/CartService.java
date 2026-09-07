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

// CONCEPT: Service layer -- business logic for managing shopping carts.
// PURPOSE: Add/remove items, get-or-create a cart per user. Reads the
// current price/name from ProductCatalog when adding an item (a snapshot,
// see CartItem), but does NOT check or reserve stock here -- stock is only
// checked at checkout (CheckoutService). That matches how a real cart
// works: you can add more than what's in stock, and find out at checkout.
// WHY the per-user lock in addItem(): several requests could add the same
// product to the same user's cart at the same time. Locking per user
// (not globally) means different users never block each other, but two
// requests for the SAME user are handled one at a time -- so merging
// quantities into one line never loses an update.
public class CartService {

    // Business rule: a single cart line can't exceed this quantity, even
    // after merging repeated "add this product again" calls.
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
