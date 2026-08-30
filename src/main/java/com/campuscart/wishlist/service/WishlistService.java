package com.campuscart.wishlist.service;

import com.campuscart.common.api.PageResponse;
import com.campuscart.common.exception.DuplicateResourceException;
import com.campuscart.common.exception.BusinessRuleException;
import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.common.exception.ProductUnavailableException;
import com.campuscart.product.domain.Product;
import com.campuscart.product.repository.ProductImageRepository;
import com.campuscart.product.service.ProductService;
import com.campuscart.notification.domain.NotificationType;
import com.campuscart.notification.service.NotificationService;
import com.campuscart.security.AuthenticatedUser;
import com.campuscart.user.domain.User;
import com.campuscart.user.service.UserService;
import com.campuscart.wishlist.domain.WishlistItem;
import com.campuscart.wishlist.dto.WishlistItemResponse;
import com.campuscart.wishlist.repository.WishlistItemRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistService {

    private final WishlistItemRepository wishlistRepository;
    private final ProductService productService;
    private final ProductImageRepository productImageRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    public WishlistService(WishlistItemRepository wishlistRepository,
                           ProductService productService,
                           ProductImageRepository productImageRepository,
                           UserService userService,
                           NotificationService notificationService) {
        this.wishlistRepository = wishlistRepository;
        this.productService = productService;
        this.productImageRepository = productImageRepository;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @Transactional
    public WishlistItemResponse add(UUID userId, UUID productId) {
        User user = userService.requireActive(userId);
        Product product = productService.requireDiscoverable(userId, productId);
        if (product.getStatus() != com.campuscart.product.domain.ProductStatus.ACTIVE) {
            throw new ProductUnavailableException("Only active products can be added to a wishlist.");
        }
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new DuplicateResourceException("Product is already in your wishlist.");
        }
        WishlistItem saved = wishlistRepository.save(new WishlistItem(user, product));
        notificationService.create(product.getSeller().getId(), NotificationType.WISHLIST_ADDED,
                "Wishlist activity", user.getFullName() + " added your product to a wishlist.",
                "{\"productId\":\"" + productId + "\",\"userId\":\"" + userId + "\"}");
        String coverUrl = getCoverImageUrl(product.getId());
        return toResponse(saved, coverUrl);
    }

    @Transactional
    public void remove(UUID userId, UUID productId) {
        WishlistItem item = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Wishlist item", productId));
        wishlistRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public PageResponse<WishlistItemResponse> list(UUID userId, int page, int size) {
        userService.requireActive(userId);
        if (page < 0 || size < 1 || size > 50) {
            throw new BusinessRuleException("Page must be non-negative and size must be between 1 and 50.");
        }
        Page<WishlistItem> pageResult = wishlistRepository.findByUserId(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<UUID> productIds = pageResult.getContent().stream()
                .map(it -> it.getProduct().getId())
                .toList();
        Map<UUID, String> coverImageMap = getCoverImageMap(productIds);

        return PageResponse.from(pageResult.map(item ->
                toResponse(item, coverImageMap.get(item.getProduct().getId()))));
    }

    @Transactional(readOnly = true)
    public boolean contains(UUID userId, UUID productId) {
        userService.requireActive(userId);
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }

    private WishlistItemResponse toResponse(WishlistItem item, String imageUrl) {
        Product product = item.getProduct();
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : null;
        String collegeName = product.getCollege() != null ? product.getCollege().getName() : null;
        String cityName = product.getCity() != null ? product.getCity().getName() : null;
        UUID sellerId = product.getSeller() != null ? product.getSeller().getId() : null;
        String sellerName = product.getSeller() != null ? product.getSeller().getFullName() : null;

        return new WishlistItemResponse(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                product.getProductType(),
                product.getSellingReach(),
                product.getStatus(),
                product.getQuantity(),
                categoryName,
                collegeName,
                cityName,
                sellerId,
                sellerName,
                imageUrl
        );
    }

    private String getCoverImageUrl(UUID productId) {
        return productImageRepository.findByProductIdInOrderByProductIdAscCreatedAtAsc(List.of(productId)).stream()
                .findFirst()
                .map(com.campuscart.product.domain.ProductImage::getDeliveryUrl)
                .orElse(null);
    }

    private Map<UUID, String> getCoverImageMap(List<UUID> productIds) {
        if (productIds.isEmpty()) return Map.of();
        return productImageRepository.findByProductIdInOrderByProductIdAscCreatedAtAsc(productIds).stream()
                .collect(Collectors.toMap(
                        img -> img.getProduct().getId(),
                        com.campuscart.product.domain.ProductImage::getDeliveryUrl,
                        (existing, replacement) -> existing
                ));
    }
}
