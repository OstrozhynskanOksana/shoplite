package com.shoplite.common.exception;

public class EmailAlreadyUsedException extends BusinessException {
    public EmailAlreadyUsedException() {
        super("A user with this email already exists");
    }
}
