package com.retailco.orderservice.repository;

import com.retailco.orderservice.model.Order;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// CONCEPT: Repository pattern -- stores orders in memory, keyed by order
// id. Production would swap this for a real database; callers wouldn't
// need to change.
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
