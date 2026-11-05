package com.shoplite.order;

import com.shoplite.cart.Cart;
import com.shoplite.cart.CartService;
import com.shoplite.common.exception.BusinessException;
import com.shoplite.common.web.CurrentUserProvider;
import com.shoplite.user.User;
import com.shoplite.user.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/** Checkout page: delivery details and order placement (FR-07, FR-08). */
@Controller
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserService userService;
    private final CurrentUserProvider currentUser;

    public CheckoutController(CartService cartService, OrderService orderService, UserService userService,
                              CurrentUserProvider currentUser) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.userService = userService;
        this.currentUser = currentUser;
    }

    @GetMapping("/checkout")
    public String checkoutPage(Principal principal, Model model) {
        User user = userService.getByEmail(principal.getName());
        Cart cart = cartService.getCart(user.getId());
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }
        CheckoutForm form = new CheckoutForm();
        form.setPhone(user.getPhone());
        model.addAttribute("form", form);
        model.addAttribute("cart", cart);
        return "order/checkout";
    }

    @PostMapping("/checkout")
    public String placeOrder(@Valid @ModelAttribute("form") CheckoutForm form, BindingResult result,
                             Principal principal, Model model, RedirectAttributes flash) {
        Long userId = currentUser.getId(principal);
        if (result.hasErrors()) {
            model.addAttribute("cart", cartService.getCart(userId));
            return "order/checkout";
        }
        try {
            Order order = orderService.placeOrder(userId, form);
            flash.addFlashAttribute("success", "Order #" + order.getId() + " has been placed");
            return "redirect:/orders/" + order.getId();
        } catch (BusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/cart";
        }
    }
}
