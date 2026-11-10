package com.paras.restaurant.exceptions;

/** The review cannot be created, edited or removed (400). */
public class ReviewNotAllowedException extends BaseException {

    public ReviewNotAllowedException(String message) {
        super(message);
    }

    public ReviewNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }
}
