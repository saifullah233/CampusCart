package com.campuscart.product.image;

import com.campuscart.common.exception.MediaStorageUnavailableException;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

final class UnavailableProductImageStorage implements ProductImageStorage {

    @Override
    public StoredImage store(UUID productId, MultipartFile file) {
        throw new MediaStorageUnavailableException();
    }

    @Override
    public void delete(String storageKey) {
        throw new MediaStorageUnavailableException();
    }
}
