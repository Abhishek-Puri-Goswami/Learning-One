package com.retailco.cartservice.service;

import com.retailco.cartservice.client.ProductCatalogClient;
import com.retailco.cartservice.dto.CartItemRequest;
import com.retailco.cartservice.dto.CartItemResponse;
import com.retailco.cartservice.dto.CartItemUpdateRequest;
import com.retailco.cartservice.dto.CartResponse;
import com.retailco.cartservice.exception.CartItemNotFoundException;
import com.retailco.cartservice.repository.CartRepository;
import com.retailco.ecommerce.model.Cart;
import com.retailco.ecommerce.model.CartItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

// CONCEPT: Service layer -- business logic between Controller and Repository.
// PURPOSE: Implements add/update/remove/get for a user's cart. Notice the
// actual cart mutation logic (merging items, removing by line id) is NOT
// reimplemented here -- it calls ecommerce-core's Cart methods
// (addOrMergeItem, setItemQuantity, removeItemById), which are already
// tested. This class's own job is: fetch the cart, call the right Cart
// method, save it back, and convert to a response DTO.
@Service
public class CartServiceImpl implements CartService {

    private final CartRepository repository;
    private final ProductCatalogClient catalogClient;
    private final Clock clock = Clock.systemUTC();

    public CartServiceImpl(CartRepository repository, ProductCatalogClient catalogClient) {
        this.repository = repository;
        this.catalogClient = catalogClient;
    }

    @Override
    public CartResponse getCart(String userId) {
        Cart cart = repository.getOrCreate(userId);
        return toResponse(cart);
    }

    @Override
    public CartResponse addItem(String userId, CartItemRequest request) {
        Cart cart = repository.getOrCreate(userId);

        ProductCatalogClient.ProductSnapshot snapshot = catalogClient.fetchProduct(request.getProductId());
        CartItem item = new CartItem(
                UUID.randomUUID().toString(),
                request.getProductId(),
                snapshot.name(),
                snapshot.price(),
                request.getQuantity()
        );
        // Cart.addOrMergeItem is ecommerce-core's tested merge logic: if this
        // productId already has a line, it adds to that line's quantity
        // instead of creating a duplicate line.
        cart.addOrMergeItem(item, Instant.now(clock));

        repository.save(cart);
        return toResponse(cart);
    }

    @Override
    public CartResponse updateItem(String userId, String itemId, CartItemUpdateRequest request) {
        Cart cart = repository.getOrCreate(userId);
        try {
            cart.setItemQuantity(itemId, request.getQuantity(), Instant.now(clock));
        } catch (NoSuchElementException e) {
            throw new CartItemNotFoundException(itemId);
        }
        repository.save(cart);
        return toResponse(cart);
    }

    @Override
    public CartResponse removeItem(String userId, String itemId) {
        Cart cart = repository.getOrCreate(userId);
        boolean removed = cart.removeItemById(itemId, Instant.now(clock));
        if (!removed) {
            throw new CartItemNotFoundException(itemId);
        }
        repository.save(cart);
        return toResponse(cart);
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(i -> new CartItemResponse(
                        i.getItemId(), i.getProductId(), i.getProductName(),
                        i.getUnitPrice(), i.getQuantity(), i.getLineTotal()))
                .toList();

        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getUserId(), items, items.size(), subtotal, cart.getUpdatedAt());
    }
}
