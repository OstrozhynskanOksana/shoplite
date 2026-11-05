package com.shoplite.cart;

import com.shoplite.common.exception.BusinessException;
import com.shoplite.common.web.CurrentUserProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/** Cart page and cart actions (FR-06). */
@Controller
public class CartController {

    private final CartService cartService;
    private final CurrentUserProvider currentUser;

    public CartController(CartService cartService, CurrentUserProvider currentUser) {
        this.cartService = cartService;
        this.currentUser = currentUser;
    }

    @GetMapping("/cart")
    public String cart(Principal principal, Model model) {
        model.addAttribute("cart", cartService.getCart(currentUser.getId(principal)));
        return "cart/cart";
    }

    @PostMapping("/cart/add")
    public String add(@RequestParam Long productId, @RequestParam(defaultValue = "1") int quantity,
                      Principal principal, RedirectAttributes flash) {
        try {
            cartService.addItem(currentUser.getId(principal), productId, Math.max(quantity, 1));
            flash.addFlashAttribute("success", "Added to cart");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/products/" + productId;
    }

    @PostMapping("/cart/items/{productId}")
    public String changeQuantity(@PathVariable Long productId, @RequestParam int quantity,
                                 Principal principal, RedirectAttributes flash) {
        try {
            cartService.changeQuantity(currentUser.getId(principal), productId, quantity);
        } catch (BusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/{productId}/remove")
    public String remove(@PathVariable Long productId, Principal principal) {
        cartService.removeItem(currentUser.getId(principal), productId);
        return "redirect:/cart";
    }
}
