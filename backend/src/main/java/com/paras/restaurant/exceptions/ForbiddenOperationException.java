package com.paras.restaurant.exceptions;

/** The caller is authenticated but is not allowed to do this (403). */
public class ForbiddenOperationException extends BaseException {

    public ForbiddenOperationException(String message) {
        super(message);
    }

    public ForbiddenOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
