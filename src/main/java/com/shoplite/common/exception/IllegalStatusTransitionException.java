package com.shoplite.common.exception;

import com.shoplite.order.OrderStatus;

public class IllegalStatusTransitionException extends BusinessException {
    public IllegalStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Cannot change order status from \"" + from.getTitle() + "\" to \"" + to.getTitle() + "\"");
    }
}
