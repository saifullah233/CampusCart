package com.campuscart.catalog.service;

import com.campuscart.catalog.domain.Category;
import com.campuscart.catalog.dto.CategoryRequest;
import com.campuscart.catalog.dto.CategoryResponse;
import com.campuscart.catalog.repository.CategoryRepository;
import com.campuscart.audit.service.AuditLogService;
import com.campuscart.common.api.PageResponse;
import com.campuscart.common.exception.BusinessRuleException;
import com.campuscart.common.exception.DuplicateResourceException;
import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.product.repository.ProductRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository,
                           AuditLogService auditLogService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(UUID id) {
        return toResponse(requireActive(id));
    }

    @Transactional
    public CategoryResponse create(UUID adminId, CategoryRequest request) {
        String name = request.name().trim();
        String slug = request.slug().trim().toLowerCase(java.util.Locale.ROOT);
        if (categoryRepository.existsByNameIgnoreCase(name) || categoryRepository.existsBySlugIgnoreCase(slug)) {
            throw new DuplicateResourceException("Category name or slug already exists.");
        }
        Category category = categoryRepository.save(new Category(name, slug));
        auditLogService.recordIfPresent(adminId, "CATEGORY_CREATED", "CATEGORY", category.getId(), "Category created.");
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse update(UUID adminId, UUID id, CategoryRequest request) {
        Category category = require(id);
        String name = request.name().trim();
        String slug = request.slug().trim().toLowerCase(java.util.Locale.ROOT);
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)
                || categoryRepository.existsBySlugIgnoreCaseAndIdNot(slug, id)) {
            throw new DuplicateResourceException("Category name or slug already exists.");
        }
        category.setName(name);
        category.setSlug(slug);
        auditLogService.recordIfPresent(adminId, "CATEGORY_UPDATED", "CATEGORY", id, "Category updated.");
        return toResponse(category);
    }

    @Transactional
    public void delete(UUID adminId, UUID id) {
        Category category = require(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessRuleException("A category assigned to products cannot be deleted.");
        }
        categoryRepository.delete(category);
        auditLogService.recordIfPresent(adminId, "CATEGORY_DELETED", "CATEGORY", id, "Category deleted.");
    }

    @Transactional
    public CategoryResponse activate(UUID adminId, UUID id) {
        Category category = require(id);
        category.activate();
        auditLogService.recordIfPresent(adminId, "CATEGORY_ACTIVATED", "CATEGORY", id, "Category activated.");
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse deactivate(UUID adminId, UUID id) {
        Category category = require(id);
        category.deactivate();
        auditLogService.recordIfPresent(adminId, "CATEGORY_DEACTIVATED", "CATEGORY", id, "Category deactivated.");
        return toResponse(category);
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> listAdmin(UUID adminId, int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new BusinessRuleException("Page must be non-negative and size must be between 1 and 50.");
        }
        return PageResponse.from(categoryRepository.findAllByOrderByNameAsc(
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name")))
                .map(this::toResponse));
    }

    public Category require(UUID id) {
        return categoryRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Category", id));
    }

    private Category requireActive(UUID id) {
        return categoryRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug(),
                category.isActive(), category.getCreatedAt(), category.getUpdatedAt());
    }
}
