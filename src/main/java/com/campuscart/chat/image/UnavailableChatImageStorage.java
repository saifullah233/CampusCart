package com.campuscart.chat.image;

import com.campuscart.common.exception.MediaStorageUnavailableException;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

final class UnavailableChatImageStorage implements ChatImageStorage {

    @Override
    public StoredImage store(UUID conversationId, MultipartFile file) {
        throw new MediaStorageUnavailableException();
    }

    @Override
    public void delete(String storageKey) {
        throw new MediaStorageUnavailableException();
    }
}
