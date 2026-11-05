package com.shoplite.order;

import java.util.Map;
import java.util.Set;

/**
 * Order statuses and the allowed transitions between them (order state diagram).
 * NEW → CONFIRMED → SHIPPED → DELIVERED; NEW / CONFIRMED → CANCELLED.
 */
public enum OrderStatus {
    NEW("New"),
    CONFIRMED("Confirmed"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            NEW, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(SHIPPED, CANCELLED),
            SHIPPED, Set.of(DELIVERED),
            DELIVERED, Set.of(),
            CANCELLED, Set.of());

    private final String title;

    OrderStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public boolean canChangeTo(OrderStatus next) {
        return TRANSITIONS.get(this).contains(next);
    }
}
