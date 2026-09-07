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

// CONCEPT: Service layer -- but this is the deliberately BUGGY "before"
// version of the checkout logic, used as a refactoring case study (see
// the FINDING comments throughout this file for each specific problem:
// missing null checks, an untested/hallucinated API call, one giant
// method doing five different jobs, and a payment result that's trusted
// even when it shouldn't be). Compare with
// order-service-refactored-uc4/.../OrderServiceImpl.java to see the fix
// for each of these.
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

    // FINDING (see reviews/): high cyclomatic/cognitive complexity (Sonar rule
    // squid:S3776, "Cognitive Complexity of methods should not be too high").
    // This single method mixes cart retrieval, discount logic, membership-tier
    // logic, address validation, and payment -- five responsibilities in one
    // ~70-line method with deeply nested conditionals. Measured cyclomatic
    // complexity: 19 (threshold for a "should fix" flag is typically 10-15).
    @SuppressWarnings("unchecked")
    @Override
    public OrderResponse checkout(OrderRequest request) {
        // FINDING: missing null check (Sonar rule squid:S2259, "Null pointers
        // should not be dereferenced"). If shippingAddress is omitted from the
        // request, this throws an unhandled NullPointerException with a 500
        // error and no useful message, instead of a clean 400 validation error.
        String city = request.getShippingAddress().getCity();

        Map<String, Object> cartSummary = cartClient.getCheckoutSummary(request.getUserId());
        // FINDING: no null check on cartSummary either -- if the (hallucinated)
        // endpoint 404s, RestTemplate throws before we even get here in some
        // configurations, but if it were ever changed to return null on empty
        // cart, the next line would NPE too.
        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) cartSummary.get("items");

        List<OrderLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map<String, Object> item : rawItems) {
            String productId = (String) item.get("productId");
            String productName = (String) item.get("productName");
            BigDecimal unitPrice = new BigDecimal(item.get("unitPrice").toString());
            int quantity = (int) item.get("quantity");

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            // Deeply nested discount / membership-tier logic (complexity driver).
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

        // FINDING: charge() return value is trusted unconditionally, and (per
        // PaymentGatewayClient's own bug) always returns true even on a
        // caught exception -- so a failed payment is recorded as a successful
        // order. This is a correctness + financial-risk defect, not just a
        // style issue.
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
