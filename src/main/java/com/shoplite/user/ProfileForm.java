package com.shoplite.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Editable profile fields (FR-11). */
public class ProfileForm {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name is too long")
    private String fullName;

    @Pattern(regexp = "^$|^\\+?[0-9 ()-]{10,20}$", message = "Invalid phone number")
    private String phone;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
