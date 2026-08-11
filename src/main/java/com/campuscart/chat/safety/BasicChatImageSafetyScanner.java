package com.campuscart.chat.safety;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Conservative baseline scanner. It catches obvious embedded markup and leaves
 * semantic NSFW classification to a provider implementation of the same interface.
 */
@Component
public class BasicChatImageSafetyScanner implements ChatImageSafetyScanner {

    @Override
    public ImageSafetyDecision scan(MultipartFile file) {
        try {
            String prefix = new String(file.getBytes(), 0, (int) Math.min(file.getSize(), 4096),
                    StandardCharsets.ISO_8859_1).toLowerCase();
            if (prefix.contains("<script") || prefix.contains("<svg") || prefix.contains("javascript:")) {
                return ImageSafetyDecision.REVIEW_REQUIRED;
            }
            return ImageSafetyDecision.CLEAR;
        } catch (IOException ex) {
            return ImageSafetyDecision.REVIEW_REQUIRED;
        }
    }
}
