package com.shoplite.common.web;

import com.shoplite.user.UserService;
import org.springframework.stereotype.Component;

import java.security.Principal;

/** Resolves the id of the logged-in user from the security principal. */
@Component
public class CurrentUserProvider {

    private final UserService userService;

    public CurrentUserProvider(UserService userService) {
        this.userService = userService;
    }

    public Long getId(Principal principal) {
        return userService.getByEmail(principal.getName()).getId();
    }
}
