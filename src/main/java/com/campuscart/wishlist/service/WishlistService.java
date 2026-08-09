package com.campuscart.wishlist.service;

import com.campuscart.common.api.PageResponse;
import com.campuscart.common.exception.DuplicateResourceException;
import com.campuscart.common.exception.BusinessRuleException;
import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.common.exception.ProductUnavailableException;
import com.campuscart.product.domain.Product;
import com.campuscart.product.service.ProductService;
import com.campuscart.security.AuthenticatedUser;
import com.campuscart.user.domain.User;
import com.campuscart.user.service.UserService;
import com.campuscart.wishlist.domain.WishlistItem;
import com.campuscart.wishlist.dto.WishlistItemResponse;
import com.campuscart.wishlist.repository.WishlistItemRepository;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistService {

    private final WishlistItemRepository wishlistRepository;
    private final ProductService productService;
    private final UserService userService;

    public WishlistService(WishlistItemRepository wishlistRepository,
                           ProductService productService,
                           UserService userService) {
        this.wishlistRepository = wishlistRepository;
        this.productService = productService;
        this.userService = userService;
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
        return toResponse(wishlistRepository.save(new WishlistItem(user, product)));
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
        return PageResponse.from(wishlistRepository.findByUserId(userId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public boolean contains(UUID userId, UUID productId) {
        userService.requireActive(userId);
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }

    private WishlistItemResponse toResponse(WishlistItem item) {
        Product product = item.getProduct();
        return new WishlistItemResponse(product.getId(), product.getTitle(), product.getPrice(),
                product.getProductType(), product.getSellingReach(), product.getStatus(), product.getQuantity());
    }
}
