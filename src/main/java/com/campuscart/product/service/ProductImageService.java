package com.campuscart.product.service;

import com.campuscart.common.exception.ImageLimitExceededException;
import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.product.domain.Product;
import com.campuscart.product.domain.ProductImage;
import com.campuscart.product.dto.ProductImageResponse;
import com.campuscart.product.image.ImageFileValidator;
import com.campuscart.product.image.ProductImageStorage;
import com.campuscart.product.repository.ProductImageRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductImageService {

    public static final int MAX_IMAGES = 5;

    private final ProductService productService;
    private final ProductImageRepository imageRepository;
    private final ProductImageStorage imageStorage;
    private final ImageFileValidator imageFileValidator;

    public ProductImageService(ProductService productService, ProductImageRepository imageRepository,
                               ProductImageStorage imageStorage, ImageFileValidator imageFileValidator) {
        this.productService = productService;
        this.imageRepository = imageRepository;
        this.imageStorage = imageStorage;
        this.imageFileValidator = imageFileValidator;
    }

    @Transactional
    public ProductImageResponse add(UUID principalId, UUID productId, MultipartFile file) {
        Product product = productService.requireWritableProduct(principalId, productId);
        long currentCount = imageRepository.countByProductId(productId);
        if (currentCount >= MAX_IMAGES) {
            throw new ImageLimitExceededException();
        }
        ImageFileValidator.ValidatedImage validated = imageFileValidator.validate(file);
        ProductImageStorage.StoredImage stored = imageStorage.store(productId, file);
        boolean isCover = (currentCount == 0);
        int displayOrder = (int) currentCount;
        ProductImage image = imageRepository.save(new ProductImage(product, stored.storageKey(),
                stored.deliveryUrl(), validated.contentType(), validated.sizeBytes(), displayOrder, isCover));
        return toResponse(image);
    }

    @Transactional
    public void delete(UUID principalId, UUID productId, UUID imageId) {
        productService.requireWritableProduct(principalId, productId);
        ProductImage image = imageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Product image", imageId));
        imageStorage.delete(image.getStorageKey());
        imageRepository.delete(image);
    }

    private ProductImageResponse toResponse(ProductImage image) {
        return new ProductImageResponse(image.getId(), image.getDeliveryUrl(), image.getDeliveryUrl(),
                image.getStorageKey(), image.getContentType(), image.getSizeBytes(),
                image.getDisplayOrder(), image.isCover(), image.getCreatedAt());
    }
}
