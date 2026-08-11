package com.campuscart.product.service;

import com.campuscart.common.exception.DuplicateResourceException;
import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.common.exception.ProductUnavailableException;
import com.campuscart.notification.domain.NotificationType;
import com.campuscart.notification.service.NotificationService;
import com.campuscart.product.domain.Product;
import com.campuscart.product.domain.ProductLike;
import com.campuscart.product.repository.ProductLikeRepository;
import com.campuscart.user.domain.User;
import com.campuscart.user.service.UserService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductLikeService {

    private final ProductLikeRepository likeRepository;
    private final ProductService productService;
    private final UserService userService;
    private final NotificationService notificationService;

    public ProductLikeService(ProductLikeRepository likeRepository, ProductService productService,
                              UserService userService, NotificationService notificationService) {
        this.likeRepository = likeRepository;
        this.productService = productService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @Transactional
    public boolean like(UUID userId, UUID productId) {
        User user = userService.requireActive(userId);
        Product product = productService.requireDiscoverable(userId, productId);
        if (product.getSeller().getId().equals(userId)) {
            throw new ProductUnavailableException("You cannot like your own product.");
        }
        if (likeRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new DuplicateResourceException("Product is already liked.");
        }
        likeRepository.save(new ProductLike(user, product));
        notificationService.create(product.getSeller().getId(), NotificationType.PRODUCT_LIKED,
                "Product liked", user.getFullName() + " liked your product.",
                "{\"productId\":\"" + productId + "\",\"userId\":\"" + userId + "\"}");
        return true;
    }

    @Transactional
    public void unlike(UUID userId, UUID productId) {
        userService.requireActive(userId);
        likeRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(likeRepository::delete);
    }

    @Transactional(readOnly = true)
    public boolean liked(UUID userId, UUID productId) {
        userService.requireActive(userId);
        return likeRepository.existsByUserIdAndProductId(userId, productId);
    }
}
