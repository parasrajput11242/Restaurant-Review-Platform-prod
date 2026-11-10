package com.paras.restaurant.services;

import com.paras.restaurant.domain.entities.Photo;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface PhotoService {

    /** Accepts JPEG, PNG and WebP only. Throws InvalidPhotoException for anything else. */
    Photo uploadPhoto(MultipartFile file);

    Optional<Resource> getPhotoAsResource(String id);
}
