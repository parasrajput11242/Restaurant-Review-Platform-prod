package com.paras.restaurant.exceptions;

/** The requested owner-access request does not exist. */
public class OwnerRequestNotFoundException extends NotFoundException {

    public OwnerRequestNotFoundException(String message) {
        super(message);
    }

    public OwnerRequestNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
