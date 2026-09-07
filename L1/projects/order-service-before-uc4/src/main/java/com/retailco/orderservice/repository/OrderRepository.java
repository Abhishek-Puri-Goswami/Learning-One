package com.retailco.orderservice.repository;

import com.retailco.orderservice.model.Order;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores orders in memory, keyed by order id. A production version of
 * this class would use a real database instead — but since callers only
 * ever talk to this class's methods, they wouldn't need to change at all
 * if we made that switch.
 */
@Repository
public class OrderRepository {

    private final Map<String, Order> store = new ConcurrentHashMap<>();

    public Order save(Order order) {
        store.put(order.getId(), order);
        return order;
    }

    public Optional<Order> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
