package com.shoplite.support;

import com.shoplite.catalog.AdminCatalogService;
import com.shoplite.catalog.Category;
import com.shoplite.catalog.Product;
import com.shoplite.catalog.ProductForm;
import com.shoplite.catalog.ProductRepository;
import com.shoplite.user.RegistrationForm;
import com.shoplite.user.User;
import com.shoplite.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Base class for component and system tests: the whole application runs on the in-memory H2 database
 * (Flyway migrations included) and requests go through the real Spring Security filter chain.
 * Every test creates its own users and products with unique names, so tests do not depend on each other.
 */
@SpringBootTest
public abstract class IntegrationTestSupport {

    protected static final String ADMIN_EMAIL = "admin@shoplite.local";
    protected static final String ADMIN_PASSWORD = "admin12345";
    protected static final String PASSWORD = "secret123";

    @Autowired
    protected WebApplicationContext context;
    @Autowired
    protected UserService userService;
    @Autowired
    protected AdminCatalogService adminCatalog;
    @Autowired
    protected ProductRepository productRepository;

    protected MockMvc mvc;

    @BeforeEach
    void setUpMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Registers a new customer through the service layer and returns the stored user. */
    protected User newCustomer() {
        RegistrationForm form = new RegistrationForm();
        form.setEmail(unique("customer") + "@example.com");
        form.setPassword(PASSWORD);
        form.setFullName("Test Customer");
        return userService.register(form);
    }

    protected Category newCategory() {
        return adminCatalog.createCategory(unique("Category"));
    }

    /** Creates an active product with the given price and stock in a new category. */
    protected Product newProduct(String price, int stock) {
        return newProduct(unique("Test product"), price, stock, newCategory().getId());
    }

    protected Product newProduct(String name, String price, int stock, Long categoryId) {
        ProductForm form = new ProductForm();
        form.setName(name);
        form.setPrice(new BigDecimal(price));
        form.setStock(stock);
        form.setCategoryId(categoryId);
        return adminCatalog.createProduct(form);
    }

    protected int stockOf(Long productId) {
        return productRepository.findById(productId).orElseThrow().getStock();
    }
}
