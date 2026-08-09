package com.campuscart.catalog.service;

import com.campuscart.catalog.domain.Category;
import com.campuscart.catalog.dto.CategoryRequest;
import com.campuscart.catalog.dto.CategoryResponse;
import com.campuscart.catalog.repository.CategoryRepository;
import com.campuscart.common.exception.BusinessRuleException;
import com.campuscart.common.exception.DuplicateResourceException;
import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.product.repository.ProductRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findAll(org.springframework.data.domain.Sort.by("name")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        String slug = request.slug().trim().toLowerCase(java.util.Locale.ROOT);
        if (categoryRepository.existsByNameIgnoreCase(name) || categoryRepository.existsBySlugIgnoreCase(slug)) {
            throw new DuplicateResourceException("Category name or slug already exists.");
        }
        return toResponse(categoryRepository.save(new Category(name, slug)));
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = require(id);
        String name = request.name().trim();
        String slug = request.slug().trim().toLowerCase(java.util.Locale.ROOT);
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)
                || categoryRepository.existsBySlugIgnoreCaseAndIdNot(slug, id)) {
            throw new DuplicateResourceException("Category name or slug already exists.");
        }
        category.setName(name);
        category.setSlug(slug);
        return toResponse(category);
    }

    @Transactional
    public void delete(UUID id) {
        Category category = require(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessRuleException("A category assigned to products cannot be deleted.");
        }
        categoryRepository.delete(category);
    }

    public Category require(UUID id) {
        return categoryRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Category", id));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug(),
                category.getCreatedAt(), category.getUpdatedAt());
    }
}
