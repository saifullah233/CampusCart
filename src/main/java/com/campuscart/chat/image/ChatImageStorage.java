package com.campuscart.chat.image;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ChatImageStorage {

    StoredImage store(UUID conversationId, MultipartFile file);

    void delete(String storageKey);

    record StoredImage(String storageKey, String deliveryUrl, String contentType, long sizeBytes) {
    }
}
