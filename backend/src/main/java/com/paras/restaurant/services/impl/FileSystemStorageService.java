package com.paras.restaurant.services.impl;

import com.paras.restaurant.config.AppProperties;
import com.paras.restaurant.exceptions.StorageException;
import com.paras.restaurant.services.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Stores photos on local disk. In Docker, mount a volume on the upload directory.
 * To use S3 or similar later, add another StorageService implementation.
 */
@Service
@Slf4j
public class FileSystemStorageService implements StorageService {

    private final Path rootLocation;

    public FileSystemStorageService(AppProperties properties) {
        this.rootLocation = Paths.get(properties.storage().location()).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage location " + rootLocation, e);
        }
    }

    @Override
    public String store(InputStream content, String filename) {
        Path destination = resolveInsideRoot(filename)
                .orElseThrow(() -> new StorageException("Cannot store file outside the storage directory"));
        try (InputStream in = content) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new StorageException("Failed to store file", e);
        }
    }

    @Override
    public Optional<Resource> loadAsResource(String filename) {
        Optional<Path> file = resolveInsideRoot(filename);
        if (file.isEmpty()) {
            return Optional.empty();
        }
        try {
            Resource resource = new UrlResource(file.get().toUri());
            return resource.exists() && resource.isReadable() ? Optional.of(resource) : Optional.empty();
        } catch (MalformedURLException e) {
            log.warn("Could not read file {}", filename, e);
            return Optional.empty();
        }
    }

    /** Resolves a file name and refuses anything that escapes the storage directory (../ tricks). */
    private Optional<Path> resolveInsideRoot(String filename) {
        if (filename == null || filename.isBlank()) {
            return Optional.empty();
        }
        Path resolved = rootLocation.resolve(filename).normalize();
        return resolved.startsWith(rootLocation) && !resolved.equals(rootLocation)
                ? Optional.of(resolved)
                : Optional.empty();
    }
}
