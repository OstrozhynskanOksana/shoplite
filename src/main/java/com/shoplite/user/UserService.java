package com.shoplite.user;

/** User business logic. */
public interface UserService {

    /** Registers a new customer (FR-01). */
    User register(RegistrationForm form);

    User getByEmail(String email);
}
