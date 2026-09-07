package com.retailco.orderservice.client;

import com.retailco.orderservice.exception.PaymentFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * L1 UC5: security/reliability scenario test -- payment timeout. Also
 * doubles as the "security scenario test" deliverable: verifies a hung/slow
 * payment gateway cannot leave the system in an ambiguous state (no silent
 * "assume success," per the L1/UC4 fix removing that exact bug).
 */
@ExtendWith(MockitoExtension.class)
class PaymentGatewayClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void charge_gatewayTimesOut_throwsPaymentFailedExceptionWithTimeoutMessage() {
        PaymentGatewayClient client = new PaymentGatewayClient(restTemplate, "test-key", "https://payments.example.com");

        when(restTemplate.postForObject(anyString(), any(), any(Class.class)))
                .thenThrow(new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> client.charge(BigDecimal.valueOf(100), "card"))
                .isInstanceOf(PaymentFailedException.class)
                .hasMessageContaining("timed out");
    }

    @Test
    void charge_gatewayReturns5xx_throwsPaymentFailedExceptionWithGenericMessage() {
        PaymentGatewayClient client = new PaymentGatewayClient(restTemplate, "test-key", "https://payments.example.com");

        when(restTemplate.postForObject(anyString(), any(), any(Class.class)))
                .thenThrow(HttpServerErrorException.InternalServerError.create(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", null, null, null));

        assertThatThrownBy(() -> client.charge(BigDecimal.valueOf(100), "card"))
                .isInstanceOf(PaymentFailedException.class)
                .hasMessageContaining("failed");
    }
}
