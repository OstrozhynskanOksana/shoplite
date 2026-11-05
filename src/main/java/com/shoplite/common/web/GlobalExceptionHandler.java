package com.shoplite.common.web;

import com.shoplite.common.exception.BusinessException;
import com.shoplite.common.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Turns business exceptions into readable error pages instead of stack traces. */
@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(NotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/not-found";
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String business(BusinessException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/business";
    }
}
