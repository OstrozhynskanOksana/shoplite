package com.shoplite.user;

/** User business logic. */
public interface UserService {

    /** Registers a new customer (FR-01). */
    User register(RegistrationForm form);

    User getByEmail(String email);

    /** Updates name and phone (FR-11). */
    void updateProfile(String email, ProfileForm form);

    /** Changes the password after checking the current one (FR-11). */
    void changePassword(String email, PasswordChangeForm form);
}
