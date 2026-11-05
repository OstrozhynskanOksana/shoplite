package com.shoplite.catalog;

import com.shoplite.common.exception.OutOfStockException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductTest {

    @Test
    void decreaseStockBelowZeroIsRejected() {
        Product p = new Product("Kettle", null, new BigDecimal("899.00"), 3, new Category("Kitchen"));
        assertThrows(OutOfStockException.class, () -> p.decreaseStock(5));
        assertEquals(3, p.getStock());
    }

    @Test
    void stockIsDecreasedAndRestored() {
        Product p = new Product("Kettle", null, new BigDecimal("899.00"), 4, new Category("Kitchen"));
        p.decreaseStock(2);
        assertEquals(2, p.getStock());
        p.increaseStock(2);
        assertEquals(4, p.getStock());
    }
}
