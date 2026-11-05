package com.shoplite.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Checkout form data (FR-07, NFR-05). */
public class CheckoutForm {

    @NotBlank(message = "Delivery address is required")
    @Size(max = 300, message = "Address is too long")
    private String deliveryAddress;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9 ()-]{10,20}$", message = "Invalid phone number")
    private String phone;

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
