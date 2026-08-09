package com.campuscart.product.image;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CloudinaryProperties.class)
public class ProductImageStorageConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "cloudinary", name = "enabled", havingValue = "true")
    ProductImageStorage cloudinaryProductImageStorage(CloudinaryProperties properties) {
        return new CloudinaryProductImageStorage(properties);
    }

    @Bean
    @ConditionalOnMissingBean(ProductImageStorage.class)
    ProductImageStorage unavailableProductImageStorage() {
        return new UnavailableProductImageStorage();
    }
}
