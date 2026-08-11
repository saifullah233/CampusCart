package com.campuscart.product.service;

import com.campuscart.admin.service.AdminAccessService;
import com.campuscart.audit.service.AuditLogService;
import com.campuscart.chat.dto.ChatReportResponse;
import com.campuscart.chat.service.ChatReportService;
import com.campuscart.common.api.PageResponse;
import com.campuscart.common.exception.BusinessRuleException;
import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.product.domain.Product;
import com.campuscart.product.domain.ProductStatus;
import com.campuscart.product.dto.AdminProductResponse;
import com.campuscart.product.dto.ProductResponse;
import com.campuscart.product.repository.ProductRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminProductModerationService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final AdminAccessService adminAccessService;
    private final AuditLogService auditLogService;
    private final ChatReportService chatReportService;

    public AdminProductModerationService(ProductRepository productRepository, ProductMapper productMapper,
                                         AdminAccessService adminAccessService, AuditLogService auditLogService,
                                         ChatReportService chatReportService) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.adminAccessService = adminAccessService;
        this.auditLogService = auditLogService;
        this.chatReportService = chatReportService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminProductResponse> list(UUID adminId, ProductStatus status, int page, int size) {
        adminAccessService.requireAdmin(adminId);
        PageRequest request = pageRequest(page, size);
        Page<Product> products = status == null
                ? productRepository.findAllByOrderByCreatedAtDesc(request)
                : productRepository.findByStatusOrderByCreatedAtDesc(status, request);
        return PageResponse.from(products.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public AdminProductResponse get(UUID adminId, UUID productId) {
        adminAccessService.requireAdmin(adminId);
        return toResponse(require(productId));
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatReportResponse> reported(UUID adminId, int page, int size) {
        return chatReportService.listReportedProducts(adminId, page, size);
    }

    @Transactional
    public AdminProductResponse hide(UUID adminId, UUID productId) {
        var admin = adminAccessService.requireAdmin(adminId);
        Product product = require(productId);
        try {
            product.deactivate();
        } catch (IllegalStateException ex) {
            throw new BusinessRuleException(ex.getMessage());
        }
        auditLogService.record(admin, "PRODUCT_HIDDEN", "PRODUCT", productId, "Product hidden by moderation.");
        return toResponse(product);
    }

    @Transactional
    public AdminProductResponse restore(UUID adminId, UUID productId) {
        var admin = adminAccessService.requireAdmin(adminId);
        Product product = require(productId);
        try {
            product.activate();
        } catch (IllegalStateException ex) {
            throw new BusinessRuleException(ex.getMessage());
        }
        auditLogService.record(admin, "PRODUCT_RESTORED", "PRODUCT", productId, "Product restored by moderation.");
        return toResponse(product);
    }

    @Transactional
    public AdminProductResponse remove(UUID adminId, UUID productId) {
        var admin = adminAccessService.requireAdmin(adminId);
        Product product = require(productId);
        product.softDelete();
        auditLogService.record(admin, "PRODUCT_REMOVED", "PRODUCT", productId, "Product removed by moderation.");
        return toResponse(product);
    }

    private Product require(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Product", id));
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new BusinessRuleException("Page must be non-negative and size must be between 1 and 50.");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private AdminProductResponse toResponse(Product product) {
        ProductResponse response = productMapper.toResponse(product);
        return new AdminProductResponse(product.getId(), response, product.getStatus(),
                product.getCreatedAt(), product.getUpdatedAt());
    }
}
