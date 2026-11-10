package com.paras.restaurant.exceptions;

/** A dependency (Keycloak, geocoder) failed or is unreachable (502). */
public class UpstreamServiceException extends BaseException {

    public UpstreamServiceException(String message) {
        super(message);
    }

    public UpstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
