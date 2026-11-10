package com.paras.restaurant.services;

import com.paras.restaurant.domain.entities.Photo;
import com.paras.restaurant.exceptions.InvalidPhotoException;
import com.paras.restaurant.services.impl.PhotoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotoServiceImplTest {

    @Mock StorageService storage;

    @Test
    void acceptsPngByContentNotByFileName() {
        when(storage.store(any(InputStream.class), endsWith(".png"))).thenAnswer(i -> i.getArgument(1));
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 1, 2};

        Photo photo = new PhotoServiceImpl(storage)
                .uploadPhoto(new MockMultipartFile("file", "evil.exe", "application/octet-stream", png));

        assertThat(photo.getUrl()).matches("[0-9a-f-]{36}\\.png");
    }

    @Test
    void rejectsNonImages() {
        byte[] text = "<?php echo 1; ?>".getBytes();

        assertThatThrownBy(() -> new PhotoServiceImpl(storage)
                .uploadPhoto(new MockMultipartFile("file", "cat.jpg", "image/jpeg", text)))
                .isInstanceOf(InvalidPhotoException.class);
    }
}
