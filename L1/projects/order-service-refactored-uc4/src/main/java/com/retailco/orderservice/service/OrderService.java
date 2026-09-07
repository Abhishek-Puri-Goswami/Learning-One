package com.retailco.orderservice.service;

import com.retailco.orderservice.dto.OrderRequest;
import com.retailco.orderservice.dto.OrderResponse;

/**
 * Lists what the order service can do. {@code OrderController} depends
 * only on this interface, not on the concrete implementation, which makes
 * it easy to swap in a different one later.
 */
public interface OrderService {
    OrderResponse checkout(OrderRequest request);
}
