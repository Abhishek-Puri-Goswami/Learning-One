package com.retailco.orderservice.service;

import com.retailco.orderservice.client.CartClient;
import com.retailco.orderservice.client.PaymentGatewayClient;
import com.retailco.orderservice.dto.OrderRequest;
import com.retailco.orderservice.dto.OrderResponse;
import com.retailco.orderservice.model.Order;
import com.retailco.orderservice.model.OrderLine;
import com.retailco.orderservice.model.ShippingAddress;
import com.retailco.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This is the "before" version of our checkout logic — kept as a teaching
 * example of a method with several real problems, each explained where it
 * happens below: a missing null check, a call to an API endpoint that
 * doesn't actually exist, one giant method trying to do five different
 * jobs at once, and a payment result that gets trusted even when it
 * shouldn't be. Compare this file with
 * {@code order-service-refactored-uc4}'s version of the same class to see
 * exactly how each of these gets fixed.
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository repository;
    private final CartClient cartClient;
    private final PaymentGatewayClient paymentGatewayClient;

    public OrderServiceImpl(OrderRepository repository, CartClient cartClient,
                             PaymentGatewayClient paymentGatewayClient) {
        this.repository = repository;
        this.cartClient = cartClient;
        this.paymentGatewayClient = paymentGatewayClient;
    }

    /**
     * This one method tries to do too much at once: fetch the cart, work
     * out discounts, apply membership-tier pricing, validate the address,
     * AND process payment — all in roughly 70 lines with several layers
     * of nested {@code if} statements. This is a common code smell called
     * "doing too many things in one place." It works, but it's hard to
     * read, hard to test one piece at a time, and easy to introduce a bug
     * into without noticing. See the refactored version of this class for
     * how splitting it into several small, focused methods fixes that.
     */
    @SuppressWarnings("unchecked")
    @Override
    public OrderResponse checkout(OrderRequest request) {
        // Here's a real bug: if the request doesn't include a shipping
        // address at all, calling .getCity() on it below crashes with a
        // NullPointerException — an ugly, unhelpful 500 error — instead of
        // a clean "please provide a shipping address" message.
        String city = request.getShippingAddress().getCity();

        Map<String, Object> cartSummary = cartClient.getCheckoutSummary(request.getUserId());
        // Same issue here: there's no check that cartSummary actually came
        // back with data before we try to read from it. If the cart lookup
        // ever returns nothing, this next line would also crash instead of
        // failing gracefully.
        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) cartSummary.get("items");

        List<OrderLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map<String, Object> item : rawItems) {
            String productId = (String) item.get("productId");
            String productName = (String) item.get("productName");
            BigDecimal unitPrice = new BigDecimal(item.get("unitPrice").toString());
            int quantity = (int) item.get("quantity");

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            // This block of nested if/else statements is exactly the kind
            // of "hard to follow" logic mentioned above — it's figuring out
            // a discount based on the coupon code, how much has been spent
            // so far, the payment method, AND the city, all tangled
            // together.
            if (request.getCouponCode() != null) {
                if (request.getCouponCode().equals("SAVE10")) {
                    if (total.compareTo(BigDecimal.valueOf(50)) > 0) {
                        if (request.getPaymentMethod().equals("card")) {
                            lineTotal = lineTotal.multiply(BigDecimal.valueOf(0.9));
                        } else {
                            if (request.getPaymentMethod().equals("upi")) {
                                lineTotal = lineTotal.multiply(BigDecimal.valueOf(0.92));
                            } else {
                                lineTotal = lineTotal.multiply(BigDecimal.valueOf(0.95));
                            }
                        }
                    } else {
                        lineTotal = lineTotal.multiply(BigDecimal.valueOf(0.97));
                    }
                } else if (request.getCouponCode().equals("VIP")) {
                    if (city != null && city.equalsIgnoreCase("Bengaluru")) {
                        lineTotal = lineTotal.multiply(BigDecimal.valueOf(0.85));
                    } else {
                        lineTotal = lineTotal.multiply(BigDecimal.valueOf(0.9));
                    }
                }
            }

            lines.add(new OrderLine(productId, productName, unitPrice, quantity));
            total = total.add(lineTotal);
        }

        // This line trusts whatever charge() returns without question.
        // Combined with PaymentGatewayClient's own bug (it always returns
        // true, even when the actual charge failed), this means a failed
        // payment could get recorded here as a successful order — a real
        // financial risk, not just a style nitpick.
        boolean paid = paymentGatewayClient.charge(total, request.getPaymentMethod());

        Order order = new Order(
                UUID.randomUUID().toString(),
                request.getUserId(),
                request.getIdempotencyKey(),
                lines,
                new ShippingAddress(
                        request.getShippingAddress().getFullName(),
                        request.getShippingAddress().getAddressLine1(),
                        city,
                        request.getShippingAddress().getPostalCode()
                ),
                request.getPaymentMethod(),
                total,
                paid ? "CONFIRMED" : "PAYMENT_FAILED",
                Instant.now()
        );
        repository.save(order);

        List<String> summaries = new ArrayList<>();
        for (OrderLine line : lines) {
            summaries.add(line.getQuantity() + "x " + line.getProductName());
        }

        return new OrderResponse(order.getId(), order.getStatus(), order.getTotal(), summaries, order.getCreatedAt());
    }
}
