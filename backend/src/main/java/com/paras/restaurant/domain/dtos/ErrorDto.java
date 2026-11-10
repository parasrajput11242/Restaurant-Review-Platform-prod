package com.paras.restaurant.domain.dtos;

import java.time.Instant;

/** Every error response from the API has this shape. */
public record ErrorDto(int status, String message, Instant timestamp) {

    public static ErrorDto of(int status, String message) {
        return new ErrorDto(status, message, Instant.now());
    }
}
