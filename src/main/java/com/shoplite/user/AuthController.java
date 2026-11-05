package com.shoplite.user;

import com.shoplite.common.exception.EmailAlreadyUsedException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/** Login and registration pages (FR-01, FR-02). Login itself is handled by Spring Security. */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(@ModelAttribute("form") RegistrationForm form) {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form, BindingResult result) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.register(form);
        } catch (EmailAlreadyUsedException ex) {
            result.rejectValue("email", "email.used", ex.getMessage());
            return "auth/register";
        }
        return "redirect:/login?registered";
    }
}
