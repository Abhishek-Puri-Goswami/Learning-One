package com.retailco.cartservice.service;

import com.retailco.cartservice.client.ProductCatalogClient;
import com.retailco.cartservice.dto.CartItemRequest;
import com.retailco.cartservice.dto.CartItemResponse;
import com.retailco.cartservice.dto.CartItemUpdateRequest;
import com.retailco.cartservice.dto.CartResponse;
import com.retailco.cartservice.exception.CartItemNotFoundException;
import com.retailco.cartservice.exception.InvalidQuantityException;
import com.retailco.cartservice.model.Cart;
import com.retailco.cartservice.model.CartItem;
import com.retailco.cartservice.repository.CartRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The business logic for a user's cart. Notice that every method wraps
 * its work in a {@code synchronized (cart)} block. That keyword means
 * "only one thread can be inside this block for this particular cart at
 * a time" — it protects us from two requests reading and then writing the
 * same cart at almost the same moment and stepping on each other's
 * changes. See the comment inside {@code addItem} below for a concrete
 * example of what could go wrong without it.
 */
@Service
public class CartServiceImpl implements CartService {

    private static final int MAX_QUANTITY_PER_LINE = 99;

    private final CartRepository repository;
    private final ProductCatalogClient catalogClient;

    public CartServiceImpl(CartRepository repository, ProductCatalogClient catalogClient) {
        this.repository = repository;
        this.catalogClient = catalogClient;
    }

    @Override
    public CartResponse getCart(String userId) {
        Cart cart = repository.getOrCreate(userId);
        synchronized (cart) {
            return toResponse(cart);
        }
    }

    @Override
    public CartResponse addItem(String userId, CartItemRequest request) {
        if (request.getQuantity() > MAX_QUANTITY_PER_LINE) {
            throw new InvalidQuantityException(request.getQuantity(), MAX_QUANTITY_PER_LINE);
        }

        Cart cart = repository.getOrCreate(userId);

        // Here's the concrete problem `synchronized` protects us from:
        // imagine two requests both trying to add the SAME product to the
        // SAME cart at nearly the same instant. Without this lock, both
        // could check "is this product already in the cart?", both see
        // "no," and both add a brand new line — instead of one request
        // adding the line and the other one correctly increasing its
        // quantity. Locking on this specific cart object means only one
        // of those two requests can be inside this block at a time, so
        // the second one always sees the first one's change before it
        // makes its own decision. Different users' carts are unaffected,
        // since each cart has its own lock.
        synchronized (cart) {
            Optional<CartItem> existing = cart.getItems().stream()
                    .filter(i -> i.getProductId().equals(request.getProductId()))
                    .findFirst();

            if (existing.isPresent()) {
                CartItem item = existing.get();
                int newQuantity = item.getQuantity() + request.getQuantity();
                if (newQuantity > MAX_QUANTITY_PER_LINE) {
                    throw new InvalidQuantityException(newQuantity, MAX_QUANTITY_PER_LINE);
                }
                item.setQuantity(newQuantity);
            } else {
                // If the product genuinely doesn't exist, fetchProduct()
                // throws InvalidProductException here. We don't catch it —
                // it's allowed to bubble all the way up to
                // GlobalExceptionHandler, which turns it into a clean 404
                // response for the caller.
                ProductCatalogClient.ProductSnapshot snapshot = catalogClient.fetchProduct(request.getProductId());
                CartItem item = new CartItem(
                        UUID.randomUUID().toString(),
                        request.getProductId(),
                        snapshot.name(),
                        snapshot.price(),
                        request.getQuantity()
                );
                cart.getItems().add(item);
            }

            cart.setUpdatedAt(Instant.now());
            repository.save(cart);
            return toResponse(cart);
        }
    }

    @Override
    public CartResponse updateItem(String userId, String itemId, CartItemUpdateRequest request) {
        Cart cart = repository.getOrCreate(userId);
        synchronized (cart) {
            CartItem item = cart.getItems().stream()
                    .filter(i -> i.getItemId().equals(itemId))
                    .findFirst()
                    .orElseThrow(() -> new CartItemNotFoundException(itemId));

            item.setQuantity(request.getQuantity());
            cart.setUpdatedAt(Instant.now());
            repository.save(cart);
            return toResponse(cart);
        }
    }

    @Override
    public CartResponse removeItem(String userId, String itemId) {
        Cart cart = repository.getOrCreate(userId);
        synchronized (cart) {
            boolean removed = cart.getItems().removeIf(i -> i.getItemId().equals(itemId));
            if (!removed) {
                throw new CartItemNotFoundException(itemId);
            }
            cart.setUpdatedAt(Instant.now());
            repository.save(cart);
            return toResponse(cart);
        }
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
