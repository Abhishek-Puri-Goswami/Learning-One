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

// A small extra thing to notice: this import isn't actually used
// anywhere in the file below. Unused imports don't break anything, but
// it's good practice to remove them so the code stays tidy and easy to
// scan.
import java.util.List;

/**
 * The HTTP entry point for placing an order. This is the "before" version
 * of this refactoring case study — see the comment on
 * {@code createOrder} below for the specific problem it has, and compare
 * with {@code order-service-refactored-uc4}'s version of this file for
 * the fix.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Notice this method has no {@code @Valid} annotation on its request
     * body, unlike product-service and cart-service's endpoints. That
     * means a malformed request (say, one missing the shipping address)
     * isn't rejected up front — it gets passed straight through to the
     * business logic, where it can cause a confusing crash instead of a
     * clean "400 Bad Request" error.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        OrderResponse response = orderService.checkout(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
