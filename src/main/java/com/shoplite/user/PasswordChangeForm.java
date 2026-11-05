package com.shoplite.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Password change form (FR-11). */
public class PasswordChangeForm {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotNull(message = "New password is required")
    @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters long")
    private String newPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
