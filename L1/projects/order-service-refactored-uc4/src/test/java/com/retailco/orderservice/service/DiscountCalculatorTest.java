package com.retailco.orderservice.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

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
    void save10_aboveThreshold_upiGetsMediumDiscount() {
        BigDecimal result = calculator.applyDiscount(
                BigDecimal.valueOf(100), "SAVE10", "upi", "Mumbai", BigDecimal.valueOf(60));
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(92));
    }

    @Test
    void vip_bengaluru_getsBiggestDiscount() {
        BigDecimal result = calculator.applyDiscount(
                BigDecimal.valueOf(100), "VIP", "card", "Bengaluru", BigDecimal.ZERO);
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(85));
    }

    @Test
    void vip_otherCity_getsStandardVipDiscount() {
        BigDecimal result = calculator.applyDiscount(
                BigDecimal.valueOf(100), "VIP", "card", "Chennai", BigDecimal.ZERO);
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(90));
    }

    @Test
    void unknownCoupon_returnsLineTotalUnchanged() {
        BigDecimal result = calculator.applyDiscount(
                BigDecimal.valueOf(100), "NOT_REAL", "card", "Chennai", BigDecimal.ZERO);
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(100));
    }
}
