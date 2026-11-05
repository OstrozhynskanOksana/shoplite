package com.shoplite.order;

import com.shoplite.common.exception.BusinessException;
import com.shoplite.common.web.CurrentUserProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/** Customer order history, order details and cancellation (FR-09, FR-10). */
@Controller
public class OrderController {

    private final OrderService orderService;
    private final CurrentUserProvider currentUser;

    public OrderController(OrderService orderService, CurrentUserProvider currentUser) {
        this.orderService = orderService;
        this.currentUser = currentUser;
    }

    @GetMapping("/orders")
    public String history(Principal principal, Model model) {
        model.addAttribute("orders", orderService.findByUser(currentUser.getId(principal)));
        return "order/list";
    }

    @GetMapping("/orders/{id}")
    public String details(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("order", orderService.getForUser(id, currentUser.getId(principal)));
        return "order/detail";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        try {
            orderService.cancel(id, currentUser.getId(principal));
            flash.addFlashAttribute("success", "Order #" + id + " has been cancelled");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }
}
