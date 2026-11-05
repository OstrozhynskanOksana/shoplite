package com.shoplite.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @Test
    void allowedTransitionsFollowStateDiagram() {
        assertTrue(OrderStatus.NEW.canChangeTo(OrderStatus.CONFIRMED));
        assertTrue(OrderStatus.NEW.canChangeTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.CONFIRMED.canChangeTo(OrderStatus.SHIPPED));
        assertTrue(OrderStatus.CONFIRMED.canChangeTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.SHIPPED.canChangeTo(OrderStatus.DELIVERED));
    }

    @Test
    void forbiddenTransitionsAreRejected() {
        assertFalse(OrderStatus.SHIPPED.canChangeTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.DELIVERED.canChangeTo(OrderStatus.NEW));
        assertFalse(OrderStatus.CANCELLED.canChangeTo(OrderStatus.CONFIRMED));
        assertFalse(OrderStatus.NEW.canChangeTo(OrderStatus.DELIVERED));
    }
}
