package com.retailco.cartservice.repository;

import com.retailco.ecommerce.model.Cart;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// CONCEPT: Repository pattern -- stores carts in memory, keyed by userId.
// PURPOSE: save/find/get-or-create a Cart. Uses ecommerce-core's Cart
// class directly rather than a separate copy, so there's only one Cart
// model in the whole system.
// WHY: production would use Redis instead (cart data is temporary and
// benefits from automatic expiry) -- callers wouldn't need to change.
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
