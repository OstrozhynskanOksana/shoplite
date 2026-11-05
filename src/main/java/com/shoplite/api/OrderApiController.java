package com.shoplite.api;

import com.shoplite.common.web.CurrentUserProvider;
import com.shoplite.order.Order;
import com.shoplite.order.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Order status API: customers see their own orders, administrators see any order. */
@RestController
public class OrderApiController {

    private final OrderService orderService;
    private final CurrentUserProvider currentUser;

    public OrderApiController(OrderService orderService, CurrentUserProvider currentUser) {
        this.orderService = orderService;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/orders/{id}/status")
    public OrderStatusDto status(@PathVariable Long id, Authentication auth) {
        boolean admin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        Order order = admin ? orderService.getById(id) : orderService.getForUser(id, currentUser.getId(auth));
        return OrderStatusDto.from(order);
    }
}
