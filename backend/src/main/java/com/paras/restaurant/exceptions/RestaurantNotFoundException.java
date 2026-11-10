package com.paras.restaurant.exceptions;

/** The requested restaurant does not exist. */
public class RestaurantNotFoundException extends NotFoundException {

    public RestaurantNotFoundException(String message) {
        super(message);
    }

    public RestaurantNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
