package com.retailco.orderservice.controller;

import com.retailco.orderservice.dto.OrderRequest;
import com.retailco.orderservice.dto.OrderResponse;
import com.retailco.orderservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// FINDING: unused import (Sonar rule squid:S1128, "Unnecessary imports should
// be removed") -- java.util.List is never referenced in this file.
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // FINDING: no @Valid / Bean Validation on the request body at all here
    // (contrast with product-service/cart-service in L1/UC2, which validate
    // every mutating endpoint) -- a malformed request reaches business logic
    // and fails with a raw NullPointerException instead of a 400.
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        OrderResponse response = orderService.checkout(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
