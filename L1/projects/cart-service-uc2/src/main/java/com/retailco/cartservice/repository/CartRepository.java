package com.retailco.cartservice.repository;

import com.retailco.ecommerce.model.Cart;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores carts in memory, one per user id. It reuses {@code ecommerce-core}'s
 * {@code Cart} class directly rather than keeping a separate copy of the
 * same idea, so there's only ever one "Cart" concept in the whole system.
 * <p>
 * A real production system would likely store carts in Redis instead,
 * since cart data is short-lived and benefits from automatically expiring
 * after a while — but whoever calls this class wouldn't need to change
 * anything if we made that switch.
 */
@Repository
public class CartRepository {

    private final Map<String, Cart> store = new ConcurrentHashMap<>();
    private final Clock clock = Clock.systemUTC();

    public Cart save(Cart cart) {
        store.put(cart.getUserId(), cart);
        return cart;
    }

    public Optional<Cart> findByUserId(String userId) {
        return Optional.ofNullable(store.get(userId));
    }

    public Cart getOrCreate(String userId) {
        return store.computeIfAbsent(userId, id -> new Cart(id, Instant.now(clock)));
    }
}
