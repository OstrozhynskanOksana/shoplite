package com.shoplite.common.exception;

/** Base exception for business rule violations. The message is shown to the user. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
