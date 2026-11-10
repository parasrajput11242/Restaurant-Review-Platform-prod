package com.paras.restaurant.exceptions;

/** Reading or writing a stored file failed (500). */
public class StorageException extends BaseException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
