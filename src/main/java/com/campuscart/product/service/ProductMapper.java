package com.campuscart.product.service;

import com.campuscart.product.domain.Product;
import com.campuscart.product.dto.ProductImageResponse;
import com.campuscart.product.dto.ProductResponse;
import com.campuscart.product.repository.ProductImageRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    private final ProductImageRepository imageRepository;

    public ProductMapper(ProductImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public ProductResponse toResponse(Product product) {
        List<ProductImageResponse> images = imageRepository.findByProductIdOrderByCreatedAtAsc(product.getId()).stream()
                .map(image -> new ProductImageResponse(image.getId(), image.getDeliveryUrl(),
                        image.getContentType(), image.getSizeBytes(), image.getCreatedAt()))
                .toList();
        return new ProductResponse(
                product.getId(),
                product.getSeller().getId(),
                product.getSeller().getFullName(),
                product.getCollege() == null ? null : product.getCollege().getId(),
                product.getCollege() == null ? null : product.getCollege().getName(),
                product.getCity().getId(),
                product.getCity().getName(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getSlug(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getProductType(),
                product.getSellingReach(),
                product.getQuantity(),
                product.getStatus(),
                images,
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getVersion());
    }
}
