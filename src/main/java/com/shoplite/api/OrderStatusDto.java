package com.shoplite.api;

import com.shoplite.order.Order;

/** Current status of an order for the REST API. */
public record OrderStatusDto(Long id, String status, String statusTitle) {

    public static OrderStatusDto from(Order order) {
        return new OrderStatusDto(order.getId(), order.getStatus().name(), order.getStatus().getTitle());
    }
}
