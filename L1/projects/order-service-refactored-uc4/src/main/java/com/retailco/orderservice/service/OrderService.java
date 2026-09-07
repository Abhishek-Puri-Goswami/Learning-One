package com.retailco.orderservice.service;

import com.retailco.orderservice.dto.OrderRequest;
import com.retailco.orderservice.dto.OrderResponse;

// CONCEPT: Service interface -- OrderController depends on this, not on
// the implementation directly.
public interface OrderService {
    OrderResponse checkout(OrderRequest request);
}
