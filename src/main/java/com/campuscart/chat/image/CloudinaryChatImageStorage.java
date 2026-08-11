package com.campuscart.chat.image;

import com.campuscart.common.exception.MediaStorageUnavailableException;
import com.campuscart.product.image.CloudinaryProperties;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

final class CloudinaryChatImageStorage implements ChatImageStorage {

    private final Cloudinary cloudinary;

    CloudinaryChatImageStorage(CloudinaryProperties properties) {
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
    public StoredImage store(UUID conversationId, MultipartFile file) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "campuscart/chats/" + conversationId,
                    "resource_type", "image",
                    "type", "authenticated",
                    "use_filename", false,
                    "unique_filename", true,
                    "overwrite", false));
            return new StoredImage(String.valueOf(result.get("public_id")),
                    String.valueOf(result.get("secure_url")), file.getContentType(), file.getSize());
        } catch (IOException | RuntimeException ex) {
            throw new MediaStorageUnavailableException(ex);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            cloudinary.uploader().destroy(storageKey, ObjectUtils.asMap(
                    "resource_type", "image", "type", "authenticated", "invalidate", true));
        } catch (IOException | RuntimeException ex) {
            throw new MediaStorageUnavailableException(ex);
        }
    }
}
