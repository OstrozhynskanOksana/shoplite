package com.shoplite.system;

import com.shoplite.cart.CartService;
import com.shoplite.catalog.Category;
import com.shoplite.catalog.CategoryRepository;
import com.shoplite.catalog.Product;
import com.shoplite.order.CheckoutForm;
import com.shoplite.order.Order;
import com.shoplite.order.OrderService;
import com.shoplite.stats.StatsService;
import com.shoplite.support.IntegrationTestSupport;
import com.shoplite.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import java.math.BigDecimal;
import java.time.YearMonth;

import static com.shoplite.support.TestData.assertMoney;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * System test: the administrator manages the catalog and processes a customer's order through the admin pages;
 * the changes are checked from the customer's side (public API) and in the monthly statistics.
 */
class AdminJourneySystemTest extends IntegrationTestSupport {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private CartService cartService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private StatsService statsService;

    private MockHttpSession logInAsAdmin() throws Exception {
        return (MockHttpSession) mvc.perform(formLogin("/login").userParameter("email")
                        .user(ADMIN_EMAIL).password(ADMIN_PASSWORD))
                .andExpect(authenticated().withRoles("ADMIN"))
                .andReturn().getRequest().getSession(false);
    }

    @Test
    @DisplayName("FR-12, FR-13: admin creates a category and a product, hides and shows the product")
    void adminManagesCatalog() throws Exception {
        MockHttpSession admin = logInAsAdmin();
        String categoryName = unique("Garden");
        String productName = unique("Watering can");

        mvc.perform(post("/admin/categories").session(admin).with(csrf()).param("name", categoryName))
                .andExpect(flash().attribute("success", "Category created"));
        mvc.perform(post("/admin/categories").session(admin).with(csrf()).param("name", categoryName.toUpperCase()))
                .andExpect(flash().attribute("error", "Category \"" + categoryName.toUpperCase() + "\" already exists"));
        Category category = categoryRepository.findAllByOrderByNameAsc().stream()
                .filter(c -> c.getName().equals(categoryName)).findFirst().orElseThrow();

        mvc.perform(post("/admin/products").session(admin).with(csrf())
                        .param("name", productName).param("price", "315.50").param("stock", "8")
                        .param("categoryId", category.getId().toString()))
                .andExpect(redirectedUrl("/admin/products"))
                .andExpect(flash().attribute("success", "Product \"" + productName + "\" created"));
        mvc.perform(get("/admin/products").session(admin))
                .andExpect(content().string(containsString(productName)));
        Product product = productRepository.findAllByOrderByNameAsc().stream()
                .filter(p -> p.getName().equals(productName)).findFirst().orElseThrow();

        // the new product is visible to customers through the public API
        mvc.perform(get("/api/products").param("q", productName))
                .andExpect(jsonPath("$.items[0].name").value(productName))
                .andExpect(jsonPath("$.items[0].price").value(315.5));

        // a category with products cannot be deleted
        mvc.perform(post("/admin/categories/{id}/delete", category.getId()).session(admin).with(csrf()))
                .andExpect(flash().attribute("error",
                        "Category \"" + categoryName + "\" has products and cannot be deleted"));

        // hidden product disappears from the catalog, then comes back
        mvc.perform(post("/admin/products/{id}/toggle", product.getId()).session(admin).with(csrf()))
                .andExpect(flash().attribute("success", "Product is hidden from the catalog"));
        mvc.perform(get("/api/products/{id}", product.getId())).andExpect(status().isNotFound());
        mvc.perform(post("/admin/products/{id}/toggle", product.getId()).session(admin).with(csrf()))
                .andExpect(flash().attribute("success", "Product is visible in the catalog again"));
        mvc.perform(get("/api/products/{id}", product.getId())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("FR-14, FR-15: admin processes an order; statistics count it, cancelled orders are excluded")
    void adminProcessesOrderAndSeesSales() throws Exception {
        YearMonth month = YearMonth.now();
        StatsService.MonthlySales before = statsService.getMonthlySales(month);

        User buyer = newCustomer();
        Product product = newProduct("120.00", 10);
        cartService.addItem(buyer.getId(), product.getId(), 2);
        CheckoutForm form = new CheckoutForm();
        form.setDeliveryAddress("Odesa, 5 Deribasivska St");
        form.setPhone("+380931234567");
        Order order = orderService.placeOrder(buyer.getId(), form);

        MockHttpSession admin = logInAsAdmin();
        mvc.perform(get("/admin/orders").param("status", "NEW").session(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(buyer.getEmail())));

        for (String status : new String[] {"CONFIRMED", "SHIPPED", "DELIVERED"}) {
            mvc.perform(post("/admin/orders/{id}/status", order.getId()).session(admin).with(csrf())
                            .param("status", status))
                    .andExpect(redirectedUrl("/admin/orders/" + order.getId()))
                    .andExpect(flash().attributeExists("success"));
        }
        mvc.perform(post("/admin/orders/{id}/status", order.getId()).session(admin).with(csrf())
                        .param("status", "CANCELLED"))
                .andExpect(flash().attribute("error", "Cannot change order status from \"Delivered\" to \"Cancelled\""));
        mvc.perform(get("/api/orders/{id}/status", order.getId()).session(admin))
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        StatsService.MonthlySales after = statsService.getMonthlySales(month);
        assertEquals(before.ordersCount() + 1, after.ordersCount());
        assertMoney(before.revenue().add(new BigDecimal("240.00")).toPlainString(), after.revenue());
        mvc.perform(get("/admin/stats").param("month", month.toString()).session(admin))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sales", after));

        // a cancelled order is not counted
        cartService.addItem(buyer.getId(), product.getId(), 1);
        Order cancelled = orderService.placeOrder(buyer.getId(), form);
        orderService.cancel(cancelled.getId(), buyer.getId());
        assertEquals(after.ordersCount(), statsService.getMonthlySales(month).ordersCount());
    }
}
