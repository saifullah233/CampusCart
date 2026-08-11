package com.campuscart.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campuscart.common.exception.InvalidImageException;
import com.campuscart.product.image.ImageFileValidator;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageFileValidatorTest {

    private final ImageFileValidator validator = new ImageFileValidator();

    @Test
    void rejectsContentTypeSpoofing() {
        MockMultipartFile spoofed = new MockMultipartFile("file", "image.png", "image/png",
                "not an image".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> validator.validate(spoofed))
                .isInstanceOf(InvalidImageException.class);
    }

    @Test
    void rejectsOversizedImage() {
        byte[] bytes = new byte[(int) ImageFileValidator.MAX_BYTES + 1];
        MockMultipartFile oversized = new MockMultipartFile("file", "image.jpg", "image/jpeg", bytes);

        assertThatThrownBy(() -> validator.validate(oversized))
                .isInstanceOf(InvalidImageException.class);
    }

    @Test
    void acceptsStructurallyValidPng() {
        byte[] onePixelPng = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMB/ax8t+UAAAAASUVORK5CYII=");

        ImageFileValidator.ValidatedImage image = validator.validate(
                new MockMultipartFile("file", "image.png", "image/png", onePixelPng));

        assertThat(image.contentType()).isEqualTo("image/png");
        assertThat(image.sizeBytes()).isEqualTo(onePixelPng.length);
    }

    @Test
    void rejectsHeaderOnlyJpeg() {
        MockMultipartFile truncated = new MockMultipartFile("file", "image.jpg", "image/jpeg",
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff});

        assertThatThrownBy(() -> validator.validate(truncated))
                .isInstanceOf(InvalidImageException.class);
    }
}
