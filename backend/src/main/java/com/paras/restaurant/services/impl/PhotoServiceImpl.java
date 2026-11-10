package com.paras.restaurant.services.impl;

import com.paras.restaurant.domain.entities.Photo;
import com.paras.restaurant.exceptions.InvalidPhotoException;
import com.paras.restaurant.exceptions.StorageException;
import com.paras.restaurant.services.PhotoService;
import com.paras.restaurant.services.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoServiceImpl implements PhotoService {

    private static final int SNIFF_BYTES = 12;

    private final StorageService storageService;

    @Override
    public Photo uploadPhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidPhotoException("Please choose an image to upload");
        }

        try (InputStream raw = file.getInputStream()) {
            // Decide the type from the file's own bytes, never from the client-supplied name or MIME type.
            byte[] header = raw.readNBytes(SNIFF_BYTES);
            String extension = detectExtension(header);
            if (extension == null) {
                throw new InvalidPhotoException("Only JPEG, PNG and WebP images are allowed");
            }

            String filename = UUID.randomUUID() + "." + extension;
            InputStream full = new SequenceInputStream(new ByteArrayInputStream(header), raw);
            String stored = storageService.store(full, filename);

            return Photo.builder()
                    .url(stored)
                    .uploadDate(LocalDateTime.now(ZoneOffset.UTC))
                    .build();
        } catch (IOException e) {
            throw new StorageException("Failed to read the uploaded file", e);
        }
    }

    @Override
    public Optional<Resource> getPhotoAsResource(String id) {
        return storageService.loadAsResource(id);
    }

    /** Magic-number check for JPEG, PNG and WebP. Returns the extension, or null if unsupported. */
    static String detectExtension(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A) {
            return "png";
        }
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
