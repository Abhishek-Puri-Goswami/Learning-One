package com.retailco.cartservice.controller;

import com.retailco.cartservice.dto.CartItemRequest;
import com.retailco.cartservice.dto.CartItemUpdateRequest;
import com.retailco.cartservice.dto.CartResponse;
import com.retailco.cartservice.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Generated to satisfy the operationIds in openapi/cart-service.yaml:
 * getCart, addItemToCart, updateCartItem, removeCartItem.
 */
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable String userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<CartResponse> addItemToCart(@PathVariable String userId,
                                                        @Valid @RequestBody CartItemRequest request) {
        CartResponse response = cartService.addItem(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{userId}/items/{itemId}")
    public ResponseEntity<CartResponse> updateCartItem(@PathVariable String userId,
                                                         @PathVariable String itemId,
                                                         @Valid @RequestBody CartItemUpdateRequest request) {
        return ResponseEntity.ok(cartService.updateItem(userId, itemId, request));
    }

    @DeleteMapping("/{userId}/items/{itemId}")
    public ResponseEntity<CartResponse> removeCartItem(@PathVariable String userId,
                                                         @PathVariable String itemId) {
        return ResponseEntity.ok(cartService.removeItem(userId, itemId));
    }
}
