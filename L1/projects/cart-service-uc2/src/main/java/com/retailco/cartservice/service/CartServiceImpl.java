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

/**
 * Delegates all cart-mutation logic (merge-on-add, remove-by-lineId,
 * update-quantity-by-lineId, subtotal) to ecommerce-core's {@link Cart}
 * domain type -- the same real, compiled-and-tested class
 * {@code ecommerce-core/reports/selftests-run-log.txt} proves correct,
 * including two tests added specifically for this use case's itemId-based
 * update/remove contract (`removeItemById`, `setItemQuantity`). Before this
 * rework, the equivalent merge/remove logic lived only here, duplicated,
 * and had never been compiled or run.
 *
 * <p>{@link ProductCatalogClient} (an HTTP call to product-service) is kept
 * as-is: that's a real microservice boundary (cart-service and
 * product-service are separate deployables), not duplicated business logic,
 * so it stays documented-but-not-compile-verified like the rest of this
 * Spring layer.
 */
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
