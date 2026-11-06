package com.shoplite.support;

import com.shoplite.catalog.Category;
import com.shoplite.catalog.Product;
import com.shoplite.user.Role;
import com.shoplite.user.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Builders for unit tests: entities get their ids the way the database would assign them. */
public final class TestData {

    private TestData() {
    }

    public static User customer(long id, String email) {
        User user = new User(email, "$2a$10$hash", "Test Customer", Role.CUSTOMER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static Category category(long id, String name) {
        Category category = new Category(name);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    public static Product product(long id, String name, String price, int stock) {
        Product product = new Product(name, null, new BigDecimal(price), stock, category(1L, "Kitchen"));
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    /** Compares money values regardless of scale (899.0 == 899.00). */
    public static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }
}
