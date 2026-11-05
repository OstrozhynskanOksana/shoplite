package com.shoplite.common.exception;

public class OutOfStockException extends BusinessException {
    public OutOfStockException(String productName) {
        super("Not enough stock for: " + productName);
    }
}
