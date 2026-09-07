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
 * FIX (was AI-QA / squid:S3776, squid:S138 "high complexity"): checkout() is
 * decomposed into small, single-purpose private methods, each independently
 * readable and testable. Discount logic moved entirely into
 * DiscountCalculator. Cyclomatic complexity of checkout() itself is now ~4
 * (down from 19); cognitive complexity ~5 (down from 24).
 *
 * FIX (was AI-SEC-2): payment failures now propagate as PaymentFailedException
 * (see PaymentGatewayClient) instead of being swallowed; an order is only ever
 * saved with status CONFIRMED after a successful charge.
 *
 * FIX (was AI-QA-1 / squid:S2259): shippingAddress is guaranteed non-null by
 * @Valid on OrderRequest (see dto/OrderRequest.java), so no defensive null
 * check is even needed here -- the validation layer owns that concern.
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

        // Propagates PaymentFailedException on any error (see PaymentGatewayClient) --
        // an order is only ever built/saved below if this line does not throw.
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
