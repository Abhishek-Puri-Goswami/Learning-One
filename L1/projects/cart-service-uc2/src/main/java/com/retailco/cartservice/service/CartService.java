package com.retailco.cartservice.service;

import com.retailco.cartservice.dto.CartItemRequest;
import com.retailco.cartservice.dto.CartItemUpdateRequest;
import com.retailco.cartservice.dto.CartResponse;

/**
 * Lists what the cart service can do. {@code CartController} depends only
 * on this interface, not on {@code CartServiceImpl} directly, which makes
 * it easy to swap in a different implementation later without touching
 * the controller.
 */
public interface CartService {

    CartResponse getCart(String userId);

    CartResponse addItem(String userId, CartItemRequest request);

    CartResponse updateItem(String userId, String itemId, CartItemUpdateRequest request);

    CartResponse removeItem(String userId, String itemId);
}
