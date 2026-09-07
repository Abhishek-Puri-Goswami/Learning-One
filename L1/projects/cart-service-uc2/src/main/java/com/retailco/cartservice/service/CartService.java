package com.retailco.cartservice.service;

import com.retailco.cartservice.dto.CartItemRequest;
import com.retailco.cartservice.dto.CartItemUpdateRequest;
import com.retailco.cartservice.dto.CartResponse;

public interface CartService {

    CartResponse getCart(String userId);

    CartResponse addItem(String userId, CartItemRequest request);

    CartResponse updateItem(String userId, String itemId, CartItemUpdateRequest request);

    CartResponse removeItem(String userId, String itemId);
}
