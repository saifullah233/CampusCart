package com.campuscart.product.image;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ProductImageStorage {

    StoredImage store(UUID productId, MultipartFile file);

    void delete(String storageKey);

    record StoredImage(String storageKey, String deliveryUrl, String contentType, long sizeBytes) {
    }
}
