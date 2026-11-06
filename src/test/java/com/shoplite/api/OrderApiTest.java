package com.shoplite.api;

import com.shoplite.cart.CartService;
import com.shoplite.catalog.Product;
import com.shoplite.order.CheckoutForm;
import com.shoplite.order.Order;
import com.shoplite.order.OrderService;
import com.shoplite.support.IntegrationTestSupport;
import com.shoplite.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Component test of the order status API and its access rules (FR-14, NFR-03). */
@Transactional
class OrderApiTest extends IntegrationTestSupport {

    @Autowired
    private CartService cartService;
    @Autowired
    private OrderService orderService;

    private User owner;
    private Order order;

    @BeforeEach
    void placeOrder() {
        owner = newCustomer();
        Product product = newProduct("75.00", 3);
        cartService.addItem(owner.getId(), product.getId(), 1);
        CheckoutForm form = new CheckoutForm();
        form.setDeliveryAddress("Lviv, 1 Market Sq");
        form.setPhone("+380501112233");
        order = orderService.placeOrder(owner.getId(), form);
    }

    @Test
    @DisplayName("NFR-03: without credentials the API answers 401, not a login page")
    void anonymousRequestIs401() throws Exception {
        mvc.perform(get("/api/orders/{id}/status", order.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIs401() throws Exception {
        mvc.perform(get("/api/orders/{id}/status", order.getId()).with(httpBasic(ADMIN_EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("FR-14: administrator sees the status of any order (HTTP Basic)")
    void adminSeesAnyOrder() throws Exception {
        mvc.perform(get("/api/orders/{id}/status", order.getId()).with(httpBasic(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId().intValue()))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.statusTitle").value("New"));
    }

    @Test
    void customerSeesOwnOrder() throws Exception {
        mvc.perform(get("/api/orders/{id}/status", order.getId()).with(user(owner.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    @DisplayName("FR-09: another customer gets 404, so order ids cannot be probed")
    void anotherCustomerGets404() throws Exception {
        User stranger = newCustomer();

        mvc.perform(get("/api/orders/{id}/status", order.getId()).with(user(stranger.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Order not found"));
    }
}
