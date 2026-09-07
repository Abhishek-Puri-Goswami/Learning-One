package com.retailco.orderservice.service;

import com.retailco.orderservice.dto.OrderRequest;
import com.retailco.orderservice.dto.OrderResponse;

public interface OrderService {
    OrderResponse checkout(OrderRequest request);
}
