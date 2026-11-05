package com.shoplite.common.exception;

public class EmptyCartException extends BusinessException {
    public EmptyCartException() {
        super("The cart is empty");
    }
}
