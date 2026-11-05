package com.shoplite.order;

import java.util.List;

/** Order business logic (see the order module class diagram). */
public interface OrderService {

    /** Places an order from the cart in a single transaction (FR-07, FR-08, NFR-04). */
    Order placeOrder(Long userId, CheckoutForm form);

    /** Cancellation by the customer, only while the order is NEW (FR-10). */
    void cancel(Long orderId, Long userId);

    /** Status change by an administrator (FR-14). */
    void changeStatus(Long orderId, OrderStatus status);

    /** Order history of a customer, newest first (FR-09). */
    List<Order> findByUser(Long userId);

    /** Order details for its owner; other users get "not found" (FR-09). */
    Order getForUser(Long orderId, Long userId);

    /** All orders, or only those with the given status, newest first (FR-14). */
    List<Order> findAll(OrderStatus status);

    /** Order details for an administrator (FR-14). */
    Order getById(Long orderId);
}
