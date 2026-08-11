package com.campuscart.chat.safety;

import org.springframework.web.multipart.MultipartFile;

public interface ChatImageSafetyScanner {

    ImageSafetyDecision scan(MultipartFile file);

    enum ImageSafetyDecision {
        CLEAR,
        REVIEW_REQUIRED
    }
}
