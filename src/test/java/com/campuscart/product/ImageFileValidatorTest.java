package com.campuscart.product;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campuscart.common.exception.InvalidImageException;
import com.campuscart.product.image.ImageFileValidator;
import java.nio.charset.StandardCharsets;
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
}
