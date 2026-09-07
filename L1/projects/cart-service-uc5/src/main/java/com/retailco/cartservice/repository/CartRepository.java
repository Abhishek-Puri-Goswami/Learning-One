package com.retailco.cartservice.repository;

import com.retailco.cartservice.model.Cart;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// CONCEPT: Repository pattern -- stores carts in memory, keyed by userId.
// WHY: production would use Redis instead, since cart data is temporary
// and benefits from automatic expiry.
@Repository
public class CartRepository {

    private final Map<String, Cart> store = new ConcurrentHashMap<>();

    public Cart save(Cart cart) {
        store.put(cart.getUserId(), cart);
        return cart;
    }

    public Optional<Cart> findByUserId(String userId) {
        return Optional.ofNullable(store.get(userId));
    }

    public Cart getOrCreate(String userId) {
        return store.computeIfAbsent(userId, Cart::new);
    }
}
