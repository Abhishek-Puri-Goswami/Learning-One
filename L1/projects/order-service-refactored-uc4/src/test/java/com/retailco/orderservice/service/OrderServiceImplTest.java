package com.retailco.orderservice.service;

import com.retailco.orderservice.client.CartClient;
import com.retailco.orderservice.client.CartClient.CartItemDto;
import com.retailco.orderservice.client.CartClient.CartSnapshot;
import com.retailco.orderservice.client.PaymentGatewayClient;
import com.retailco.orderservice.dto.OrderRequest;
import com.retailco.orderservice.dto.OrderResponse;
import com.retailco.orderservice.exception.EmptyCartException;
import com.retailco.orderservice.exception.PaymentFailedException;
import com.retailco.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
 * L1 UC4 deliverable: regression coverage for the previously-untested
 * checkout() flow (AI-QA-4 in reviews/ai-review-report.json: "0% coverage").
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository repository;
    @Mock private CartClient cartClient;
    @Mock private PaymentGatewayClient paymentGatewayClient;

    private DiscountCalculator discountCalculator; // real instance, it's pure logic
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        discountCalculator = new DiscountCalculator();
        orderService = new OrderServiceImpl(repository, cartClient, paymentGatewayClient, discountCalculator);
    }

    @Test
    void checkout_happyPath_returnsConfirmedOrder() {
        when(cartClient.getCart("user-1")).thenReturn(new CartSnapshot(List.of(
                new CartItemDto("i1", "p1", "Wireless Mouse", BigDecimal.valueOf(20), 2, BigDecimal.valueOf(40))
        )));
        doNothing().when(paymentGatewayClient).charge(any(), anyString());

        OrderRequest request = validRequest("user-1", null);

        OrderResponse response = orderService.checkout(request);

        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(40));
        verify(repository).save(any());
        verify(paymentGatewayClient).charge(BigDecimal.valueOf(40), "card");
    }

    @Test
    void checkout_emptyCart_throwsWithoutCallingPayment() {
        when(cartClient.getCart("user-2")).thenReturn(new CartSnapshot(List.of()));

        OrderRequest request = validRequest("user-2", null);

        assertThatThrownBy(() -> orderService.checkout(request))
                .isInstanceOf(EmptyCartException.class);

        verifyNoInteractions(paymentGatewayClient);
        verify(repository, never()).save(any());
    }

    @Test
    void checkout_paymentGatewayFails_orderIsNeverSaved() {
        when(cartClient.getCart("user-3")).thenReturn(new CartSnapshot(List.of(
                new CartItemDto("i1", "p1", "Keyboard", BigDecimal.valueOf(60), 1, BigDecimal.valueOf(60))
        )));
        doThrow(new PaymentFailedException("gateway down", new RuntimeException()))
                .when(paymentGatewayClient).charge(any(), anyString());

        OrderRequest request = validRequest("user-3", null);

        // Regression test for AI-SEC-2: a failed payment must propagate, not
        // be silently recorded as a confirmed order.
        assertThatThrownBy(() -> orderService.checkout(request))
                .isInstanceOf(PaymentFailedException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void checkout_save10Coupon_appliesCardDiscountAboveThreshold() {
        when(cartClient.getCart("user-4")).thenReturn(new CartSnapshot(List.of(
                new CartItemDto("i1", "p1", "Monitor", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(100))
        )));

        OrderRequest request = validRequest("user-4", "SAVE10");

        OrderResponse response = orderService.checkout(request);

        // 100 * 0.90 = 90 (card, above the 50 threshold)
        assertThat(response.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(90));
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
