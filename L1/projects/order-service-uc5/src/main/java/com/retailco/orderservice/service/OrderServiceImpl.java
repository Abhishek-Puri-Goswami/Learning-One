package com.retailco.orderservice.service;

import com.retailco.orderservice.client.CartClient;
import com.retailco.orderservice.client.CartClient.CartItemDto;
import com.retailco.orderservice.client.PaymentGatewayClient;
import com.retailco.orderservice.client.ProductClient;
import com.retailco.orderservice.dto.OrderRequest;
import com.retailco.orderservice.dto.OrderResponse;
import com.retailco.orderservice.exception.EmptyCartException;
import com.retailco.orderservice.exception.OutOfStockException;
import com.retailco.orderservice.model.Order;
import com.retailco.orderservice.model.OrderLine;
import com.retailco.orderservice.model.ShippingAddress;
import com.retailco.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runs the checkout process, step by step: fetch the cart, check stock,
 * work out pricing, charge the customer, and save the order. Each step
 * has its own small, focused private method below. The newest addition
 * here is {@code validateStock} — it re-checks real, current stock
 * immediately before payment is attempted, so we never accidentally
 * charge a customer for something that's actually sold out.
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository repository;
    private final CartClient cartClient;
    private final ProductClient productClient;
    private final PaymentGatewayClient paymentGatewayClient;
    private final DiscountCalculator discountCalculator;

    public OrderServiceImpl(OrderRepository repository, CartClient cartClient, ProductClient productClient,
                             PaymentGatewayClient paymentGatewayClient, DiscountCalculator discountCalculator) {
        this.repository = repository;
        this.cartClient = cartClient;
        this.productClient = productClient;
        this.paymentGatewayClient = paymentGatewayClient;
        this.discountCalculator = discountCalculator;
    }

    @Override
    public OrderResponse checkout(OrderRequest request) {
        List<CartItemDto> cartItems = fetchNonEmptyCart(request.getUserId());
        validateStock(cartItems);

        String city = request.getShippingAddress().getCity();
        PricedLines pricedLines = priceLines(cartItems, request.getCouponCode(),
                request.getPaymentMethod(), city);

        // If the charge fails or times out, this line throws — and the
        // order below only ever gets built and saved once we're sure the
        // payment actually went through.
        paymentGatewayClient.charge(pricedLines.total(), request.getPaymentMethod());

        Order order = buildConfirmedOrder(request, pricedLines);
        repository.save(order);

        return toResponse(order);
    }

    private List<CartItemDto> fetchNonEmptyCart(String userId) {
        List<CartItemDto> items = cartClient.getCart(userId).items();
        if (items == null || items.isEmpty()) {
            throw new EmptyCartException(userId);
        }
        return items;
    }

    /**
     * Re-checks real, current stock for every item right before we
     * charge the customer. The cart's own quantity is only a snapshot
     * taken when the item was added, and stock can easily change by the
     * time someone actually checks out — this is where we catch that and
     * stop the order instead of quietly charging for something that's no
     * longer available.
     */
    private void validateStock(List<CartItemDto> cartItems) {
        for (CartItemDto item : cartItems) {
            int available = productClient.getAvailableStock(item.productId());
            if (item.quantity() > available) {
                throw new OutOfStockException(item.productId(), item.quantity(), available);
            }
        }
    }

    private PricedLines priceLines(List<CartItemDto> cartItems, String couponCode,
                                    String paymentMethod, String city) {
        List<OrderLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItemDto item : cartItems) {
            BigDecimal lineTotal = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            lineTotal = discountCalculator.applyDiscount(lineTotal, couponCode, paymentMethod, city, total);

            lines.add(new OrderLine(item.productId(), item.productName(), item.unitPrice(), item.quantity()));
            total = total.add(lineTotal);
        }

        return new PricedLines(lines, total);
    }

    private Order buildConfirmedOrder(OrderRequest request, PricedLines pricedLines) {
        return new Order(
                UUID.randomUUID().toString(),
                request.getUserId(),
                request.getIdempotencyKey(),
                pricedLines.lines(),
                new ShippingAddress(
                        request.getShippingAddress().getFullName(),
                        request.getShippingAddress().getAddressLine1(),
                        request.getShippingAddress().getCity(),
                        request.getShippingAddress().getPostalCode()
                ),
                request.getPaymentMethod(),
                pricedLines.total(),
                "CONFIRMED",
                Instant.now()
        );
    }

    private OrderResponse toResponse(Order order) {
        List<String> summaries = order.getLines().stream()
                .map(line -> line.getQuantity() + "x " + line.getProductName())
                .toList();
        return new OrderResponse(order.getId(), order.getStatus(), order.getTotal(), summaries, order.getCreatedAt());
    }

    private record PricedLines(List<OrderLine> lines, BigDecimal total) {
    }
}
