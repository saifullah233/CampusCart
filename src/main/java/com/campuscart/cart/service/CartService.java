package com.campuscart.cart.service;

import com.campuscart.cart.domain.CartItem;
import com.campuscart.cart.dto.AddCartItemRequest;
import com.campuscart.cart.dto.CartItemResponse;
import com.campuscart.cart.dto.CartResponse;
import com.campuscart.cart.dto.UpdateCartItemRequest;
import com.campuscart.cart.repository.CartItemRepository;
import com.campuscart.common.exception.ProductUnavailableException;
import com.campuscart.product.domain.Product;
import com.campuscart.product.domain.ProductStatus;
import com.campuscart.product.repository.ProductRepository;
import com.campuscart.product.service.ProductService;
import com.campuscart.user.domain.User;
import com.campuscart.user.service.UserService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartItemRepository cartRepository;
    private final ProductService productService;
    private final ProductRepository productRepository;
    private final UserService userService;

    public CartService(CartItemRepository cartRepository, ProductService productService,
                       ProductRepository productRepository, UserService userService) {
        this.cartRepository = cartRepository;
        this.productService = productService;
        this.productRepository = productRepository;
        this.userService = userService;
    }

    @Transactional
    public CartItemResponse add(UUID userId, AddCartItemRequest request) {
        User user = userService.requireActive(userId);
        productService.requireDiscoverable(userId, request.productId());
        Product product = productRepository.findByIdForUpdate(request.productId())
                .orElseThrow(() -> new ProductUnavailableException("Product is no longer available."));
        CartItem item = cartRepository.findByUserIdAndProductIdForUpdate(userId, request.productId()).orElse(null);
        int requested = request.quantity() + (item == null ? 0 : item.getQuantity());
        validatePurchase(user, product, requested);
        if (item == null) {
            item = new CartItem(user, product, requested);
        } else {
            item.changeQuantity(requested);
        }
        return toResponse(cartRepository.save(item));
    }

    @Transactional
    public CartItemResponse update(UUID userId, UUID productId, UpdateCartItemRequest request) {
        User user = userService.requireActive(userId);
        productService.requireDiscoverable(userId, productId);
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ProductUnavailableException("Product is no longer available."));
        CartItem item = cartRepository.findByUserIdAndProductIdForUpdate(userId, productId)
                .orElseThrow(() -> new ProductUnavailableException("Product is not in your cart."));
        validatePurchase(user, product, request.quantity());
        item.changeQuantity(request.quantity());
        return toResponse(item);
    }

    @Transactional
    public void remove(UUID userId, UUID productId) {
        userService.requireActive(userId);
        cartRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Transactional(readOnly = true)
    public CartResponse get(UUID userId) {
        userService.requireActive(userId);
        List<CartItemResponse> items = cartRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(this::toResponse)
                .toList();
        BigDecimal total = items.stream().filter(CartItemResponse::available)
                .map(CartItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(items, total, !items.isEmpty() && items.stream().allMatch(CartItemResponse::available));
    }

    private void validatePurchase(User user, Product product, int requestedQuantity) {
        if (product.getSeller().getId().equals(user.getId())) {
            throw new ProductUnavailableException("You cannot purchase your own product.");
        }
        if (requestedQuantity < 1 || product.getStatus() != ProductStatus.ACTIVE
                || product.getQuantity() < requestedQuantity) {
            throw new ProductUnavailableException("Product is unavailable for the requested quantity.");
        }
    }

    private CartItemResponse toResponse(CartItem item) {
        Product product = item.getProduct();
        boolean available = !product.getSeller().getId().equals(item.getUser().getId())
                && product.getStatus() == ProductStatus.ACTIVE
                && product.getQuantity() >= item.getQuantity();
        BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(product.getId(), product.getTitle(), product.getSeller().getId(),
                product.getSeller().getFullName(), product.getPrice(), item.getQuantity(), lineTotal,
                product.getStatus(), product.getQuantity(), available);
    }
}
