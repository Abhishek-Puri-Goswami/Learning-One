package com.retailco.orderservice.service;

import com.retailco.orderservice.exception.InvalidCouponException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscountCalculatorTest {

    private final DiscountCalculator calculator = new DiscountCalculator();

    @Test
    void noCoupon_returnsLineTotalUnchanged() {
        BigDecimal result = calculator.applyDiscount(BigDecimal.valueOf(50), null, "card", "Mumbai", BigDecimal.ZERO);
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    void save10_belowThreshold_appliesFlatSmallDiscount() {
        BigDecimal result = calculator.applyDiscount(
                BigDecimal.valueOf(30), "SAVE10", "card", "Mumbai", BigDecimal.valueOf(10));
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(30).multiply(BigDecimal.valueOf(0.97)));
    }

    @Test
    void save10_aboveThreshold_cardGetsBiggestDiscount() {
        BigDecimal result = calculator.applyDiscount(
                BigDecimal.valueOf(100), "SAVE10", "card", "Mumbai", BigDecimal.valueOf(60));
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(90));
    }

    @Test
    void vip_bengaluru_getsBiggestDiscount() {
        BigDecimal result = calculator.applyDiscount(
                BigDecimal.valueOf(100), "VIP", "card", "Bengaluru", BigDecimal.ZERO);
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(85));
    }

    // --- Invalid coupon (L1/UC5 edge case) ----------------------------------

    @Test
    void unknownCoupon_throwsInvalidCouponException() {
        assertThatThrownBy(() -> calculator.applyDiscount(
                BigDecimal.valueOf(100), "SAVE1O", "card", "Chennai", BigDecimal.ZERO)) // typo: letter O not zero
                .isInstanceOf(InvalidCouponException.class)
                .hasMessageContaining("SAVE1O");
    }

    @Test
    void expiredOrRetiredCoupon_throwsInvalidCouponException() {
        assertThatThrownBy(() -> calculator.applyDiscount(
                BigDecimal.valueOf(100), "SUMMER2024", "card", "Chennai", BigDecimal.ZERO))
                .isInstanceOf(InvalidCouponException.class);
    }

    @Test
    void blankCouponCode_treatedAsNoCoupon_notAnError() {
        BigDecimal result = calculator.applyDiscount(BigDecimal.valueOf(100), "", "card", "Chennai", BigDecimal.ZERO);
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(100));
    }
}
