package com.shoplite.admin;

import com.shoplite.common.exception.BusinessException;
import com.shoplite.order.OrderService;
import com.shoplite.order.OrderStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Order processing in the admin panel (FR-14). */
@Controller
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/admin")
    public String home() {
        return "redirect:/admin/orders";
    }

    @GetMapping("/admin/orders")
    public String list(@RequestParam(required = false) OrderStatus status, Model model) {
        model.addAttribute("orders", orderService.findAll(status));
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("selectedStatus", status);
        return "admin/orders";
    }

    @GetMapping("/admin/orders/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getById(id));
        return "admin/order";
    }

    @PostMapping("/admin/orders/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam OrderStatus status, RedirectAttributes flash) {
        try {
            orderService.changeStatus(id, status);
            flash.addFlashAttribute("success", "Status changed to \"" + status.getTitle() + "\"");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}
