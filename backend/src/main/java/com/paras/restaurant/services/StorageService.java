package com.paras.restaurant.services;

import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.Optional;

public interface StorageService {

    /** Stores the bytes under the exact file name given and returns that name. */
    String store(InputStream content, String filename);

    Optional<Resource> loadAsResource(String filename);
}
