package com.campuscart.product.image;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.campuscart.common.exception.MediaStorageUnavailableException;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

final class CloudinaryProductImageStorage implements ProductImageStorage {

    private final Cloudinary cloudinary;

    CloudinaryProductImageStorage(CloudinaryProperties properties) {
        if (!StringUtils.hasText(properties.getCloudName())
                || !StringUtils.hasText(properties.getApiKey())
                || !StringUtils.hasText(properties.getApiSecret())) {
            throw new IllegalStateException("Cloudinary is enabled but credentials are incomplete.");
        }
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", properties.getCloudName(),
                "api_key", properties.getApiKey(),
                "api_secret", properties.getApiSecret(),
                "secure", true));
    }

    @Override
    public StoredImage store(UUID productId, MultipartFile file) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "campuscart/products/" + productId,
                    "resource_type", "image",
                    "type", "authenticated",
                    "use_filename", false,
                    "unique_filename", true,
                    "overwrite", false));
            return new StoredImage(
                    String.valueOf(result.get("public_id")),
                    String.valueOf(result.get("secure_url")),
                    file.getContentType(),
                    file.getSize());
        } catch (IOException | RuntimeException ex) {
            throw new MediaStorageUnavailableException(ex);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            cloudinary.uploader().destroy(storageKey, ObjectUtils.asMap(
                    "resource_type", "image",
                    "type", "authenticated",
                    "invalidate", true));
        } catch (IOException | RuntimeException ex) {
            throw new MediaStorageUnavailableException(ex);
        }
    }
}
