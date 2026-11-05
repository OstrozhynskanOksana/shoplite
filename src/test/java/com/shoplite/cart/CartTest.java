package com.shoplite.cart;

import com.shoplite.catalog.Category;
import com.shoplite.catalog.Product;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartTest {

    private Product product(long id, String price) {
        Product p = new Product("Product " + id, null, new BigDecimal(price), 10, new Category("Kitchen"));
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Test
    void addingSameProductIncreasesQuantity() {
        Cart cart = new Cart(null);
        Product kettle = product(1, "899.00");
        cart.addItem(kettle, 1);
        cart.addItem(kettle, 2);
        assertEquals(1, cart.getItems().size());
        assertEquals(3, cart.getItems().get(0).getQuantity());
    }

    @Test
    void totalIsSumOfSubtotals() {
        Cart cart = new Cart(null);
        cart.addItem(product(1, "899.00"), 2);
        cart.addItem(product(2, "185.50"), 1);
        assertEquals(new BigDecimal("1983.50"), cart.getTotal());
    }

    @Test
    void zeroQuantityRemovesItem() {
        Cart cart = new Cart(null);
        cart.addItem(product(1, "100.00"), 1);
        cart.changeQuantity(1L, 0);
        assertTrue(cart.isEmpty());
    }
}
