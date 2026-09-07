package com.retailco.orderservice.model;

// CONCEPT: Domain model -- a simple value holder for where an order ships.
public class ShippingAddress {
    private String fullName;
    private String addressLine1;
    private String city;
    private String postalCode;

    public ShippingAddress() {
    }

    public ShippingAddress(String fullName, String addressLine1, String city, String postalCode) {
        this.fullName = fullName;
        this.addressLine1 = addressLine1;
        this.city = city;
        this.postalCode = postalCode;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
}
