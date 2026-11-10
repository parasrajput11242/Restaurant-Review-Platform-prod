package com.paras.restaurant.exceptions;

/** The uploaded file is not an acceptable image (400). */
public class InvalidPhotoException extends BaseException {

    public InvalidPhotoException(String message) {
        super(message);
    }

    public InvalidPhotoException(String message, Throwable cause) {
        super(message, cause);
    }
}
