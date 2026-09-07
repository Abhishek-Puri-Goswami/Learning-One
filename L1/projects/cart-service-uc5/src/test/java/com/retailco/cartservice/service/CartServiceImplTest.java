package com.retailco.cartservice.service;

import com.retailco.cartservice.client.ProductCatalogClient;
import com.retailco.cartservice.client.ProductCatalogClient.ProductSnapshot;
import com.retailco.cartservice.dto.CartItemRequest;
import com.retailco.cartservice.dto.CartResponse;
import com.retailco.cartservice.exception.InvalidProductException;
import com.retailco.cartservice.exception.InvalidQuantityException;
import com.retailco.cartservice.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * L1 UC5 deliverable: JUnit test suite for CartService, generated per the use
 * case's exact prompt ("Generate JUnit test cases for CartService. Cover:
 * Normal flow, Empty cart, Large quantity, Concurrent modification, Invalid
 * product ID."). Each test below is documented in
 * edge-cases/edge-case-catalog.md with: test name, scenario, expected
 * result -- this file is the corresponding code snippet for each entry.
 */
@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock private ProductCatalogClient catalogClient;

    private CartRepository repository; // real in-memory repo, not mocked -- we want real state
    private CartServiceImpl cartService;

    @BeforeEach
    void setUp() {
        repository = new CartRepository();
        cartService = new CartServiceImpl(repository, catalogClient);
    }

    // --- 1. Normal flow --------------------------------------------------

    @Test
    void normalFlow_addSingleItem_returnsCartWithCorrectSubtotal() {
        when(catalogClient.fetchProduct("p1")).thenReturn(new ProductSnapshot("Wireless Mouse", BigDecimal.valueOf(20)));

        CartItemRequest request = new CartItemRequest();
        request.setProductId("p1");
        request.setQuantity(2);

        CartResponse response = cartService.addItem("user-1", request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(40));
        assertThat(response.getItemCount()).isEqualTo(1);
    }

    // --- 2. Empty cart -----------------------------------------------------

    @Test
    void emptyCart_forNewUser_returnsZeroItemsAndZeroSubtotal() {
        CartResponse response = cartService.getCart("brand-new-user");

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getItemCount()).isZero();
        assertThat(response.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // --- 3. Large quantity (boundary test) ----------------------------------

    @Test
    void largeQuantity_atCap_isAccepted() {
        when(catalogClient.fetchProduct("p1")).thenReturn(new ProductSnapshot("Bulk Item", BigDecimal.valueOf(1)));

        CartItemRequest request = new CartItemRequest();
        request.setProductId("p1");
        request.setQuantity(99); // exactly at the cap -- boundary value

        CartResponse response = cartService.addItem("user-2", request);

        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(99);
    }

    @Test
    void largeQuantity_overCap_throwsInvalidQuantityException() {
        CartItemRequest request = new CartItemRequest();
        request.setProductId("p1");
        request.setQuantity(999_999); // scripted/fat-fingered "large quantity" attack

        assertThatThrownBy(() -> cartService.addItem("user-3", request))
                .isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    void largeQuantity_sumAcrossTwoAdds_overCap_throwsOnSecondAdd() {
        when(catalogClient.fetchProduct("p1")).thenReturn(new ProductSnapshot("Item", BigDecimal.ONE));

        CartItemRequest first = new CartItemRequest();
        first.setProductId("p1");
        first.setQuantity(60);
        cartService.addItem("user-4", first);

        CartItemRequest second = new CartItemRequest();
        second.setProductId("p1");
        second.setQuantity(60); // 60 + 60 = 120 > 99 cap

        assertThatThrownBy(() -> cartService.addItem("user-4", second))
                .isInstanceOf(InvalidQuantityException.class);
    }

    // --- 4. Concurrent modification -----------------------------------------

    @Test
    void concurrentModification_sameProductAddedFromManyThreads_noLostUpdates() throws InterruptedException {
        when(catalogClient.fetchProduct("p1")).thenReturn(new ProductSnapshot("Contested Item", BigDecimal.ONE));

        int threadCount = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                CartItemRequest request = new CartItemRequest();
                request.setProductId("p1");
                request.setQuantity(1);
                ready.countDown();
                try {
                    go.await(); // release all threads at (almost) the same instant
                    cartService.addItem("shared-user", request);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(2, TimeUnit.SECONDS);
        go.countDown(); // fire the starting gun
        done.await(5, TimeUnit.SECONDS);
        pool.shutdown();

        CartResponse finalCart = cartService.getCart("shared-user");

        // Regression test for the lost-update race documented in
        // edge-cases/edge-case-catalog.md: without the `synchronized (cart)`
        // block in CartServiceImpl.addItem, this assertion is flaky and
        // intermittently fails (final quantity < 20, or multiple separate
        // line items for the same productId instead of one summed line).
        assertThat(finalCart.getItems()).hasSize(1);
        assertThat(finalCart.getItems().get(0).getQuantity()).isEqualTo(threadCount);
    }

    // --- 5. Invalid product ID ----------------------------------------------

    @Test
    void invalidProductId_productNotFoundInCatalog_throwsInvalidProductException() {
        when(catalogClient.fetchProduct("does-not-exist")).thenThrow(new InvalidProductException("does-not-exist"));

        CartItemRequest request = new CartItemRequest();
        request.setProductId("does-not-exist");
        request.setQuantity(1);

        // Regression test for the original bug: this used to silently add
        // the item at price 0 via a fallback snapshot instead of rejecting
        // it. See ProductCatalogClient and edge-cases/edge-case-catalog.md.
        assertThatThrownBy(() -> cartService.addItem("user-5", request))
                .isInstanceOf(InvalidProductException.class);
    }
}
