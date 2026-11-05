package com.shoplite.user;

import com.shoplite.common.exception.BusinessException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/** Profile page: name, phone and password change (FR-11). */
@Controller
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        User user = userService.getByEmail(principal.getName());
        ProfileForm form = new ProfileForm();
        form.setFullName(user.getFullName());
        form.setPhone(user.getPhone());
        model.addAttribute("profileForm", form);
        model.addAttribute("passwordForm", new PasswordChangeForm());
        model.addAttribute("email", user.getEmail());
        return "user/profile";
    }

    @PostMapping("/profile")
    public String update(@Valid @ModelAttribute("profileForm") ProfileForm form, BindingResult result,
                         Principal principal, Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("passwordForm", new PasswordChangeForm());
            model.addAttribute("email", principal.getName());
            return "user/profile";
        }
        userService.updateProfile(principal.getName(), form);
        flash.addFlashAttribute("success", "Profile updated");
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
                                 BindingResult result, Principal principal, Model model,
                                 RedirectAttributes flash) {
        if (!result.hasErrors()) {
            try {
                userService.changePassword(principal.getName(), form);
                flash.addFlashAttribute("success", "Password changed");
                return "redirect:/profile";
            } catch (BusinessException ex) {
                result.rejectValue("currentPassword", "password.wrong", ex.getMessage());
            }
        }
        User user = userService.getByEmail(principal.getName());
        ProfileForm profileForm = new ProfileForm();
        profileForm.setFullName(user.getFullName());
        profileForm.setPhone(user.getPhone());
        model.addAttribute("profileForm", profileForm);
        model.addAttribute("email", user.getEmail());
        return "user/profile";
    }
}
