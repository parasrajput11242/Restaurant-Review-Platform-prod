package com.paras.restaurant.controllers;

import com.paras.restaurant.domain.dtos.PhotoDto;
import com.paras.restaurant.domain.entities.Photo;
import com.paras.restaurant.mappers.PhotoMapper;
import com.paras.restaurant.services.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/photos")
public class PhotoController {

    private final PhotoService photoService;
    private final PhotoMapper photoMapper;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public PhotoDto uploadPhoto(@RequestParam("file") MultipartFile file) {
        Photo savedPhoto = photoService.uploadPhoto(file);
        return photoMapper.toDto(savedPhoto);
    }

    @GetMapping(path = "/{id:.+}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String id) {
        return photoService.getPhotoAsResource(id)
                .map(photo -> ResponseEntity.ok()
                        .contentType(MediaTypeFactory.getMediaType(photo).orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .header("X-Content-Type-Options", "nosniff")
                        // file names are random UUIDs and never reused, so they can be cached for long
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                        .body(photo))
                .orElse(ResponseEntity.notFound().build());
    }
}
