package com.retailco.cartservice.repository;

import com.retailco.ecommerce.model.Cart;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository for this contract-first scaffold, now backed by
 * ecommerce-core's real, compiled-and-tested {@link Cart} domain type
 * (see ecommerce-core/reports/) instead of a hand-rolled, never-run
 * duplicate. This use case's own local {@code cartservice.model.Cart}/
 * {@code CartItem} classes were deleted in this rework rather than kept as
 * a parallel, drifting copy of the same shape -- one real Cart model, not
 * two claimed ones.
 *
 * <p>Production target is still Redis (per ADR-002 in L1/UC1), since cart
 * state is ephemeral and benefits from TTL-based expiry -- unchanged from
 * the original scaffold's plan.
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
