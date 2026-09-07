package com.retailco.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// CONCEPT: Request DTO with Bean Validation -- now with @NotNull/@Valid/
// @Pattern annotations checked automatically before this data reaches
// business logic.
/**
 * FIX (was AI-QA-1 / squid:S2259, AI-QA-2): shippingAddress is now @NotNull
 * and @Valid so a missing/incomplete address is rejected with a clean 400
 * before it ever reaches OrderServiceImpl, instead of throwing a raw NPE.
 */
public class OrderRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;

    @NotNull(message = "shippingAddress is required")
    @Valid
    private ShippingAddressDto shippingAddress;

    @NotBlank(message = "paymentMethod is required")
    @Pattern(regexp = "card|upi", message = "paymentMethod must be 'card' or 'upi'")
    private String paymentMethod;

    private String couponCode;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public ShippingAddressDto getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(ShippingAddressDto shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public static class ShippingAddressDto {
        @NotBlank(message = "fullName is required")
        private String fullName;

        @NotBlank(message = "addressLine1 is required")
        private String addressLine1;

        @NotBlank(message = "city is required")
        private String city;

        @NotBlank(message = "postalCode is required")
        private String postalCode;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getAddressLine1() { return addressLine1; }
        public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    }
}
