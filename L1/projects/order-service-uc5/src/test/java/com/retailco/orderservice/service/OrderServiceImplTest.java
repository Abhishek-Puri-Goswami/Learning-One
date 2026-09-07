package com.retailco.orderservice.service;

import com.retailco.orderservice.client.CartClient;
import com.retailco.orderservice.client.CartClient.CartItemDto;
import com.retailco.orderservice.client.CartClient.CartSnapshot;
import com.retailco.orderservice.client.PaymentGatewayClient;
import com.retailco.orderservice.client.ProductClient;
import com.retailco.orderservice.dto.OrderRequest;
import com.retailco.orderservice.dto.OrderResponse;
import com.retailco.orderservice.exception.EmptyCartException;
import com.retailco.orderservice.exception.InvalidCouponException;
import com.retailco.orderservice.exception.OutOfStockException;
import com.retailco.orderservice.exception.PaymentFailedException;
import com.retailco.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * L1 UC5: order-service checkout() edge-case simulation, per the brief's
 * "The AI Must: Simulate: Out-of-stock, Payment timeout, Invalid coupon."
 * Each test is documented with a matching entry in
 * edge-cases/edge-case-catalog.md (test name, scenario, expected result).
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository repository;
    @Mock private CartClient cartClient;
    @Mock private ProductClient productClient;
    @Mock private PaymentGatewayClient paymentGatewayClient;

    private DiscountCalculator discountCalculator;
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        discountCalculator = new DiscountCalculator();
        orderService = new OrderServiceImpl(repository, cartClient, productClient, paymentGatewayClient, discountCalculator);
    }

    @Test
    void checkout_happyPath_returnsConfirmedOrder() {
        when(cartClient.getCart("user-1")).thenReturn(new CartSnapshot(List.of(
                new CartItemDto("i1", "p1", "Wireless Mouse", BigDecimal.valueOf(20), 2, BigDecimal.valueOf(40))
        )));
        when(productClient.getAvailableStock("p1")).thenReturn(10);
        doNothing().when(paymentGatewayClient).charge(any(), anyString());

        OrderResponse response = orderService.checkout(validRequest("user-1", null));

        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(40));
        verify(repository).save(any());
    }

    @Test
    void checkout_emptyCart_throwsWithoutCheckingStockOrPayment() {
        when(cartClient.getCart("user-2")).thenReturn(new CartSnapshot(List.of()));

        assertThatThrownBy(() -> orderService.checkout(validRequest("user-2", null)))
                .isInstanceOf(EmptyCartException.class);

        verifyNoInteractions(productClient, paymentGatewayClient);
        verify(repository, never()).save(any());
    }

    // --- Out-of-stock -----------------------------------------------------

    @Test
    void checkout_requestedQuantityExceedsAvailableStock_throwsOutOfStockBeforePayment() {
        when(cartClient.getCart("user-3")).thenReturn(new CartSnapshot(List.of(
                new CartItemDto("i1", "p1", "Limited Edition Headphones", BigDecimal.valueOf(150), 5, BigDecimal.valueOf(750))
        )));
        when(productClient.getAvailableStock("p1")).thenReturn(2); // only 2 left, but cart wants 5

        assertThatThrownBy(() -> orderService.checkout(validRequest("user-3", null)))
                .isInstanceOf(OutOfStockException.class)
                .hasMessageContaining("requested 5")
                .hasMessageContaining("only 2 available");

        // Regression test for the original gap: payment must never be
        // attempted for stock that is no longer available.
        verifyNoInteractions(paymentGatewayClient);
        verify(repository, never()).save(any());
    }

    @Test
    void checkout_stockExactlyMatchesRequestedQuantity_boundaryIsAccepted() {
        when(cartClient.getCart("user-3b")).thenReturn(new CartSnapshot(List.of(
                new CartItemDto("i1", "p1", "Last Unit Item", BigDecimal.valueOf(10), 3, BigDecimal.valueOf(30))
        )));
        when(productClient.getAvailableStock("p1")).thenReturn(3); // exactly enough -- boundary

        OrderResponse response = orderService.checkout(validRequest("user-3b", null));

        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
    }

    // --- Payment timeout ----------------------------------------------------

    @Test
    void checkout_paymentGatewayTimesOut_propagatesPaymentFailedException_orderNeverSaved() {
        when(cartClient.getCart("user-4")).thenReturn(new CartSnapshot(List.of(
                new CartItemDto("i1", "p1", "Keyboard", BigDecimal.valueOf(60), 1, BigDecimal.valueOf(60))
        )));
        when(productClient.getAvailableStock("p1")).thenReturn(10);
        doThrow(new PaymentFailedException("Payment gateway timed out for amount 60 via card", new RuntimeException()))
                .when(paymentGatewayClient).charge(any(), anyString());

        assertThatThrownBy(() -> orderService.checkout(validRequest("user-4", null)))
                .isInstanceOf(PaymentFailedException.class)
                .hasMessageContaining("timed out");

        verify(repository, never()).save(any());
    }

    // --- Invalid coupon -----------------------------------------------------

    @Test
    void checkout_unrecognizedCoupon_throwsInvalidCouponException_beforePayment() {
        when(cartClient.getCart("user-5")).thenReturn(new CartSnapshot(List.of(
                new CartItemDto("i1", "p1", "Notebook", BigDecimal.valueOf(5), 4, BigDecimal.valueOf(20))
        )));
        when(productClient.getAvailableStock("p1")).thenReturn(10);

        assertThatThrownBy(() -> orderService.checkout(validRequest("user-5", "SAVE1O"))) // typo'd coupon
                .isInstanceOf(InvalidCouponException.class);

        verifyNoInteractions(paymentGatewayClient);
        verify(repository, never()).save(any());
    }

    @Test
    void checkout_validCoupon_appliesDiscountAndConfirms() {
        when(cartClient.getCart("user-6")).thenReturn(new CartSnapshot(List.of(
                new CartItemDto("i1", "p1", "Monitor", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(100))
        )));
        when(productClient.getAvailableStock("p1")).thenReturn(10);

        OrderResponse response = orderService.checkout(validRequest("user-6", "SAVE10"));

        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(90)); // card, above threshold
    }

    private OrderRequest validRequest(String userId, String couponCode) {
        OrderRequest request = new OrderRequest();
        request.setUserId(userId);
        request.setIdempotencyKey("idem-" + userId);
        request.setPaymentMethod("card");
        request.setCouponCode(couponCode);

        OrderRequest.ShippingAddressDto address = new OrderRequest.ShippingAddressDto();
        address.setFullName("Test User");
        address.setAddressLine1("123 Test St");
        address.setCity("Bengaluru");
        address.setPostalCode("560001");
        request.setShippingAddress(address);

        return request;
    }
}
