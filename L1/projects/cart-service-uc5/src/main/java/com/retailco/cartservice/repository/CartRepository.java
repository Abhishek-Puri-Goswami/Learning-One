package com.retailco.cartservice.repository;

import com.retailco.cartservice.model.Cart;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository for this contract-first scaffold. Production target is
 * Redis (per ADR-002 in L1/UC1) since cart state is ephemeral and benefits
 * from TTL-based expiry.
 */
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
