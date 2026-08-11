package com.campuscart.chat.image;

import com.campuscart.product.image.CloudinaryProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatImageStorageConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "cloudinary", name = "enabled", havingValue = "true")
    ChatImageStorage cloudinaryChatImageStorage(CloudinaryProperties properties) {
        return new CloudinaryChatImageStorage(properties);
    }

    @Bean
    @ConditionalOnMissingBean(ChatImageStorage.class)
    ChatImageStorage unavailableChatImageStorage() {
        return new UnavailableChatImageStorage();
    }
}
