package com.retailco.orderservice.service;

import com.retailco.orderservice.client.CartClient;
import com.retailco.orderservice.client.CartClient.CartItemDto;
import com.retailco.orderservice.client.PaymentGatewayClient;
import com.retailco.orderservice.dto.OrderRequest;
import com.retailco.orderservice.dto.OrderResponse;
import com.retailco.orderservice.exception.EmptyCartException;
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
 * This is the fixed version of our checkout logic. Instead of one long
 * method trying to do everything, {@code checkout()} now reads almost
 * like a checklist, and each step has its own small, focused method below
 * it: {@code fetchNonEmptyCart}, {@code priceLines},
 * {@code buildConfirmedOrder}, {@code toResponse}. All the discount math
 * has moved out entirely into {@code DiscountCalculator}. Each of these
 * pieces can now be read, understood, and tested completely on its own.
 * <p>
 * Two other things worth noticing: a failed payment now throws a
 * {@code PaymentFailedException} instead of being silently ignored — so
 * an order is only ever saved once we KNOW the charge succeeded. And
 * there's no manual null check for the shipping address here anymore,
 * because {@code @Valid} on {@code OrderRequest} already guarantees it
 * can't be missing by the time this code runs.
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository repository;
    private final CartClient cartClient;
    private final PaymentGatewayClient paymentGatewayClient;
    private final DiscountCalculator discountCalculator;

    public OrderServiceImpl(OrderRepository repository, CartClient cartClient,
                             PaymentGatewayClient paymentGatewayClient,
                             DiscountCalculator discountCalculator) {
        this.repository = repository;
        this.cartClient = cartClient;
        this.paymentGatewayClient = paymentGatewayClient;
        this.discountCalculator = discountCalculator;
    }

    @Override
    public OrderResponse checkout(OrderRequest request) {
        List<CartItemDto> cartItems = fetchNonEmptyCart(request.getUserId());
        String city = request.getShippingAddress().getCity();

        PricedLines pricedLines = priceLines(cartItems, request.getCouponCode(),
                request.getPaymentMethod(), city);

        // If the charge fails, this line throws PaymentFailedException,
        // which stops execution right here — the order below only ever
        // gets built and saved if this call succeeds.
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
