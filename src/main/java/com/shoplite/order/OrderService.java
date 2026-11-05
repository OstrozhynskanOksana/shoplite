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

    List<Order> findByUser(Long userId);
}
