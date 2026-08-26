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

    @Test
    void acceptsStructurallyValidJpeg() throws Exception {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "jpg", baos);
        byte[] jpegBytes = baos.toByteArray();

        ImageFileValidator.ValidatedImage image = validator.validate(
                new MockMultipartFile("file", "image.jpg", "image/jpeg", jpegBytes));

        assertThat(image.contentType()).isEqualTo("image/jpeg");
        assertThat(image.sizeBytes()).isEqualTo(jpegBytes.length);
    }

    @Test
    void rejectsUnsupportedMimeType() {
        MockMultipartFile gif = new MockMultipartFile("file", "image.gif", "image/gif",
                new byte[]{'G', 'I', 'F', '8', '9', 'a'});

        assertThatThrownBy(() -> validator.validate(gif))
                .isInstanceOf(InvalidImageException.class);
    }
}
