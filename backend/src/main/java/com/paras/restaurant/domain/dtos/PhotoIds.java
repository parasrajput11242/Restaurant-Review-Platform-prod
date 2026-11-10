package com.paras.restaurant.domain.dtos;

/** Photo ids are the generated file names: a UUID plus an image extension. */
public final class PhotoIds {

    public static final String PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(jpg|png|webp)$";

    private PhotoIds() {
    }
}
