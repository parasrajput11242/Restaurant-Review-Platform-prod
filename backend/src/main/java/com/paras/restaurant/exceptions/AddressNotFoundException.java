package com.paras.restaurant.exceptions;

/** The address could not be located on a map (422). */
public class AddressNotFoundException extends BaseException {

    public AddressNotFoundException(String message) {
        super(message);
    }

    public AddressNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
