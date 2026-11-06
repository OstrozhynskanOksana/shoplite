package com.shoplite.system;

import com.shoplite.catalog.Product;
import com.shoplite.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * System test: the customer's way through the shop over HTTP, as a browser would do it —
 * sign-up form, login form, session cookie, CSRF tokens, pages and redirects.
 * Nothing is mocked and nothing is rolled back.
 */
class CustomerJourneySystemTest extends IntegrationTestSupport {

    private Product product;
    private String email;

    @BeforeEach
    void prepareProduct() {
        product = newProduct("249.00", 5);
        email = unique("journey") + "@example.com";
    }

    private MockHttpSession signUpAndLogIn() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("email", email)
                        .param("password", PASSWORD)
                        .param("fullName", "Journey Customer"))
                .andExpect(redirectedUrl("/login?registered"));

        MvcResult login = mvc.perform(formLogin("/login").userParameter("email").user(email).password(PASSWORD))
                .andExpect(redirectedUrl("/products"))
                .andExpect(authenticated().withUsername(email).withRoles("CUSTOMER"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);
        return session;
    }

    @Test
    @DisplayName("FR-01 – FR-10: sign up, log in, fill the cart, place and cancel an order, log out")
    void customerBuysAndCancels() throws Exception {
        MockHttpSession session = signUpAndLogIn();

        // FR-03, FR-05: the product is in the catalog and has an "Add to cart" button
        mvc.perform(get("/products").param("q", product.getName()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(product.getName())));
        mvc.perform(get("/products/{id}", product.getId()).session(session))
                .andExpect(content().string(containsString("Add to cart")));

        // FR-06: add 2 items, then try to add more than the stock
        mvc.perform(post("/cart/add").session(session).with(csrf())
                        .param("productId", product.getId().toString()).param("quantity", "2"))
                .andExpect(redirectedUrl("/products/" + product.getId()))
                .andExpect(flash().attribute("success", "Added to cart"));
        mvc.perform(post("/cart/add").session(session).with(csrf())
                        .param("productId", product.getId().toString()).param("quantity", "4"))
                .andExpect(flash().attribute("error", "Only 5 pcs of \"" + product.getName() + "\" left in stock"));
        mvc.perform(get("/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(product.getName())))
                .andExpect(content().string(containsString("498.00")));

        // FR-07, NFR-05: an invalid phone is rejected, a valid form places the order
        mvc.perform(post("/checkout").session(session).with(csrf())
                        .param("deliveryAddress", "Kyiv, 37 Beresteiskyi Ave").param("phone", "abc"))
                .andExpect(view().name("order/checkout"))
                .andExpect(model().attributeHasFieldErrors("form", "phone"));
        MvcResult placed = mvc.perform(post("/checkout").session(session).with(csrf())
                        .param("deliveryAddress", "Kyiv, 37 Beresteiskyi Ave").param("phone", "+380 67 123 4567"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/orders/*"))
                .andReturn();
        String orderUrl = placed.getResponse().getRedirectedUrl();
        String orderId = orderUrl.substring(orderUrl.lastIndexOf('/') + 1);

        // FR-08: stock is taken, the cart is empty
        assertEquals(3, stockOf(product.getId()));
        mvc.perform(get("/checkout").session(session)).andExpect(redirectedUrl("/cart"));

        // FR-09: the order is in the history and its page shows status "New"
        mvc.perform(get("/orders").session(session))
                .andExpect(content().string(containsString("#" + orderId)));
        mvc.perform(get(orderUrl).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("New")));
        mvc.perform(get("/api/orders/{id}/status", orderId).session(session))
                .andExpect(jsonPath("$.status").value("NEW"));

        // FR-10: cancel the order, stock comes back; a second cancel is refused
        mvc.perform(post(orderUrl + "/cancel").session(session).with(csrf()))
                .andExpect(redirectedUrl(orderUrl))
                .andExpect(flash().attribute("success", "Order #" + orderId + " has been cancelled"));
        assertEquals(5, stockOf(product.getId()));
        mvc.perform(post(orderUrl + "/cancel").session(session).with(csrf()))
                .andExpect(flash().attribute("error", "Only a new order can be cancelled"));
        mvc.perform(get("/api/orders/{id}/status", orderId).session(session))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // FR-02: after logout the cart is closed again (the browser asks for HTML, so it is redirected)
        mvc.perform(post("/logout").session(session).with(csrf()).accept(MediaType.TEXT_HTML))
                .andExpect(redirectedUrl("/products?logout"));
        mvc.perform(get("/cart").session(session).accept(MediaType.TEXT_HTML))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("FR-02: a wrong password does not log in and does not say which field is wrong")
    void wrongPasswordIsRejected() throws Exception {
        signUpAndLogIn();

        mvc.perform(formLogin("/login").userParameter("email").user(email).password("wrong-password"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
        mvc.perform(formLogin("/login").userParameter("email").user("nobody@example.com").password(PASSWORD))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("FR-11: profile changes and the new password works at the next login")
    void customerChangesProfileAndPassword() throws Exception {
        MockHttpSession session = signUpAndLogIn();

        mvc.perform(post("/profile").session(session).with(csrf())
                        .param("fullName", "Renamed Customer").param("phone", "+380 50 111 2233"))
                .andExpect(redirectedUrl("/profile"));
        mvc.perform(get("/profile").session(session))
                .andExpect(content().string(containsString("Renamed Customer")));

        mvc.perform(post("/profile/password").session(session).with(csrf())
                        .param("currentPassword", "not-my-password").param("newPassword", "brandnew123"))
                .andExpect(model().attributeHasFieldErrorCode("passwordForm", "currentPassword", "password.wrong"));
        mvc.perform(post("/profile/password").session(session).with(csrf())
                        .param("currentPassword", PASSWORD).param("newPassword", "brandnew123"))
                .andExpect(redirectedUrl("/profile"));

        mvc.perform(formLogin("/login").userParameter("email").user(email).password("brandnew123"))
                .andExpect(authenticated());
        mvc.perform(formLogin("/login").userParameter("email").user(email).password(PASSWORD))
                .andExpect(unauthenticated());
    }
}
