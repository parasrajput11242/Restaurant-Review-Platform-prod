package com.paras.restaurant.exceptions;

/** The requested review does not exist. */
public class ReviewNotFoundException extends NotFoundException {

    public ReviewNotFoundException(String message) {
        super(message);
    }

    public ReviewNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
