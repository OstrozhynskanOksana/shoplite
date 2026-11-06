package com.shoplite.config;

import com.shoplite.support.IntegrationTestSupport;
import com.shoplite.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Component test of access rules and form validation on the web pages (NFR-03, NFR-05). */
@Transactional
class WebSecurityTest extends IntegrationTestSupport {

    @Test
    void catalogIsPublic() throws Exception {
        mvc.perform(get("/products")).andExpect(status().isOk()).andExpect(view().name("catalog/list"));
        mvc.perform(get("/products/1")).andExpect(status().isOk()).andExpect(view().name("catalog/product"));
        mvc.perform(get("/")).andExpect(redirectedUrl("/products"));
    }

    @Test
    @DisplayName("NFR-03: cart, orders and profile require login")
    void privatePagesRedirectToLogin() throws Exception {
        for (String url : new String[] {"/cart", "/orders", "/checkout", "/profile"}) {
            // a browser asks for HTML and is sent to the login page
            mvc.perform(get(url).accept(MediaType.TEXT_HTML))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
            // a client that does not ask for HTML (curl, Postman) gets 401 instead of an HTML redirect
            mvc.perform(get(url)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("NFR-03: a customer gets 403 on the admin panel")
    void customerCannotOpenAdminPanel() throws Exception {
        mvc.perform(get("/admin/orders").with(user("buyer@example.com").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/products/1/toggle").with(user("buyer@example.com").roles("CUSTOMER")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminOpensAdminPanel() throws Exception {
        mvc.perform(get("/admin/orders").with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/orders"));
        mvc.perform(get("/admin").with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(redirectedUrl("/admin/orders"));
    }

    @Test
    @DisplayName("NFR-03: forms without a CSRF token are rejected")
    void postWithoutCsrfTokenIsRejected() throws Exception {
        User buyer = newCustomer();

        mvc.perform(post("/cart/add").param("productId", "1").with(user(buyer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("NFR-05: registration errors are shown next to the fields")
    void registrationValidation() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("email", "not-an-email")
                        .param("password", "short")
                        .param("fullName", " "))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("form", "email", "password", "fullName"));
    }

    @Test
    @DisplayName("FR-01: an email can be registered only once")
    void duplicateEmailIsRejected() throws Exception {
        User existing = newCustomer();

        mvc.perform(post("/register").with(csrf())
                        .param("email", existing.getEmail().toUpperCase())
                        .param("password", PASSWORD)
                        .param("fullName", "Someone Else"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("form", "email", "email.used"));
    }

    @Test
    @DisplayName("NFR-05: checkout validates the phone number")
    void checkoutValidation() throws Exception {
        User buyer = newCustomer();

        mvc.perform(post("/checkout").with(csrf()).with(user(buyer.getEmail()).roles("CUSTOMER"))
                        .param("deliveryAddress", "")
                        .param("phone", "12-ab"))
                .andExpect(status().isOk())
                .andExpect(view().name("order/checkout"))
                .andExpect(model().attributeHasFieldErrors("form", "deliveryAddress", "phone"));
    }

    @Test
    @DisplayName("NFR-05: admin product form rejects price 0 and negative stock")
    void productFormValidation() throws Exception {
        mvc.perform(post("/admin/products").with(csrf()).with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .param("name", "")
                        .param("price", "0")
                        .param("stock", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(model().attributeHasFieldErrors("form", "name", "price", "stock", "categoryId"));
    }

    @Test
    void unknownPagesShowNotFound() throws Exception {
        mvc.perform(get("/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/not-found"));
    }
}
