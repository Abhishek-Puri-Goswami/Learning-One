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

// CONCEPT: Service layer -- business logic between Controller and
// Repository, using `synchronized` blocks to make each cart's
// read-then-write operations thread-safe (see the comment inside
// addItem() below for exactly why this matters).
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

        // L1/UC5 fix (see edge-cases/edge-case-catalog.md, "Concurrent
        // modification"): the original implementation read `existing`,
        // decided add-vs-update, and mutated the list with NO synchronization.
        // Two threads adding the same product at (almost) the same time could
        // both observe "not present" and each append a separate CartItem --
        // a classic lost-update race -- instead of the quantities being
        // summed. Synchronizing on the per-user Cart instance makes
        // read-decide-write atomic for that user's cart without a
        // service-wide lock (different users' carts are unaffected).
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
                // Throws InvalidProductException for a genuine 404 (L1/UC5 fix,
                // see ProductCatalogClient) -- propagates out of this
                // synchronized block and out of addItem() uncaught, to be
                // mapped to a 404 by GlobalExceptionHandler.
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
