package com.paras.restaurant.exceptions;

/** The request conflicts with the current state (409). */
public class ConflictException extends BaseException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
