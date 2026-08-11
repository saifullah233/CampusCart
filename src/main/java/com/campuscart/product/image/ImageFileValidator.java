package com.campuscart.product.image;

import com.campuscart.common.exception.InvalidImageException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageFileValidator {

    public static final long MAX_BYTES = 5 * 1024 * 1024;
    private static final long MAX_PIXELS = 20_000_000;
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "image/jpeg",
            "image/png", "image/png",
            "image/webp", "image/webp");

    public ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("Image file is required.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new InvalidImageException("Image file must not exceed 5 MB.");
        }
        String contentType = ALLOWED_TYPES.get(file.getContentType());
        if (contentType == null) {
            throw new InvalidImageException("Only JPEG, PNG, and WEBP images are supported.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new InvalidImageException("Image file could not be read.");
        }
        if (!matchesSignature(contentType, bytes)) {
            throw new InvalidImageException("Image content does not match its declared type.");
        }
        validateStructure(contentType, bytes);
        return new ValidatedImage(contentType, bytes.length);
    }

    private boolean matchesSignature(String contentType, byte[] bytes) {
        if ("image/jpeg".equals(contentType)) {
            return bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
                    && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
        }
        if ("image/png".equals(contentType)) {
            return bytes.length >= 8 && Arrays.equals(
                    Arrays.copyOf(bytes, 8), new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
        }
        return bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I'
                && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    private void validateStructure(String contentType, byte[] bytes) {
        if ("image/webp".equals(contentType)) {
            if (!hasValidWebpContainer(bytes)) {
                throw new InvalidImageException("Image content is not a valid WEBP file.");
            }
            return;
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new InvalidImageException("Image content could not be parsed.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new InvalidImageException("Image content could not be parsed.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                    throw new InvalidImageException("Image dimensions are invalid.");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw new InvalidImageException("Image content could not be parsed.");
        }
    }

    private boolean hasValidWebpContainer(byte[] bytes) {
        int declaredSize = littleEndianInt(bytes, 4);
        boolean declaredLengthMatches = declaredSize >= 4 && declaredSize + 8 == bytes.length;
        boolean supportedChunk = bytes.length >= 16
                && ((bytes[12] == 'V' && bytes[13] == 'P' && bytes[14] == '8' && bytes[15] == ' ')
                || (bytes[12] == 'V' && bytes[13] == 'P' && bytes[14] == '8' && bytes[15] == 'L')
                || (bytes[12] == 'V' && bytes[13] == 'P' && bytes[14] == '8' && bytes[15] == 'X'));
        return declaredLengthMatches && supportedChunk;
    }

    private int littleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff)
                | ((bytes[offset + 1] & 0xff) << 8)
                | ((bytes[offset + 2] & 0xff) << 16)
                | ((bytes[offset + 3] & 0xff) << 24);
    }

    public record ValidatedImage(String contentType, long sizeBytes) {
    }
}
