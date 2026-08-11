package com.campuscart.product.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuscart.catalog.domain.Category;
import com.campuscart.catalog.repository.CategoryRepository;
import com.campuscart.common.api.PageResponse;
import com.campuscart.common.exception.AccountNotActiveException;
import com.campuscart.common.exception.BusinessRuleException;
import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.notification.service.NotificationService;
import com.campuscart.product.domain.MarketplaceScope;
import com.campuscart.product.domain.Product;
import com.campuscart.product.domain.ProductStatus;
import com.campuscart.product.domain.ProductType;
import com.campuscart.product.domain.SellingReach;
import com.campuscart.product.dto.CreateProductRequest;
import com.campuscart.product.dto.ProductResponse;
import com.campuscart.product.dto.ProductSearchQuery;
import com.campuscart.product.dto.UpdateProductRequest;
import com.campuscart.product.repository.ProductRepository;
import com.campuscart.product.repository.ProductSpecifications;
import com.campuscart.user.domain.User;
import com.campuscart.user.repository.UserRepository;

@Service
public class ProductService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;
    private final com.campuscart.product.repository.ProductImageRepository productImageRepository;
    private final NotificationService notificationService;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          UserRepository userRepository, ProductMapper productMapper,
                          com.campuscart.product.repository.ProductImageRepository productImageRepository,
                          NotificationService notificationService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.productMapper = productMapper;
        this.productImageRepository = productImageRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ProductResponse create(UUID principalId, CreateProductRequest request) {
        User seller = requireActiveUser(principalId);
        Category category = requireCategory(request.categoryId());
        int quantity = request.quantity() == null ? 1 : request.quantity();
        validateReach(seller, request.sellingReach());
        Product product = new Product(seller, seller.getCollege(), seller.getCity(), category,
                request.title().trim(), request.description().trim(), request.price(),
                request.productType(), request.sellingReach(), quantity);
        Product saved = productRepository.save(product);
        notificationService.notifyNewProduct(saved);
        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse update(UUID principalId, UUID productId, UpdateProductRequest request) {
        Product product = requireWritableProduct(principalId, productId);
        if (product.getStatus() == ProductStatus.DELETED) {
            throw new BusinessRuleException("A deleted product cannot be updated.");
        }
        User seller = product.getSeller();
        Category category = request.categoryId() == null ? product.getCategory() : requireCategory(request.categoryId());
        String title = request.title() == null ? product.getTitle() : request.title().trim();
        String description = request.description() == null ? product.getDescription() : request.description().trim();
        BigDecimal price = request.price() == null ? product.getPrice() : request.price();
        ProductType productType = request.productType() == null ? product.getProductType() : request.productType();
        SellingReach sellingReach = request.sellingReach() == null ? product.getSellingReach() : request.sellingReach();
        int quantity = request.quantity() == null ? product.getQuantity() : request.quantity();
        validateReach(seller, sellingReach);
        product.updateDetails(category, title, description, price, productType, sellingReach, quantity);
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse get(UUID principalId, UUID productId) {
        User viewer = requireActiveUser(principalId);
        Product product = requireProduct(productId);
        if (!canDiscover(product, viewer, MarketplaceScope.ALL_PRODUCTS)) {
            throw ResourceNotFoundException.of("Product", productId);
        }
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(UUID principalId, ProductSearchQuery query) {
        User viewer = requireActiveUser(principalId);
        validateQuery(query);
        MarketplaceScope scope = query.scope() == null ? MarketplaceScope.ALL_PRODUCTS : query.scope();
        var specification = ProductSpecifications.visibleTo(viewer, scope, query.status());
        var filters = ProductSpecifications.allOf(java.util.Arrays.asList(
                ProductSpecifications.keyword(query.keyword()),
                ProductSpecifications.category(query.categoryId()),
                ProductSpecifications.productType(query.productType()),
                ProductSpecifications.sellingReach(query.sellingReach()),
                ProductSpecifications.college(query.collegeId()),
                ProductSpecifications.city(query.cityId()),
                ProductSpecifications.minimumPrice(query.minPrice()),
                ProductSpecifications.maximumPrice(query.maxPrice())));
        if (filters != null) {
            specification = specification.and(filters);
        }
        var page = productRepository.findAll(specification,
                PageRequest.of(query.page(), query.size(), parseSort(query.sort())));

        // Bulk-load associations and images for the page content to avoid N+1 queries
        java.util.List<java.util.UUID> ids = page.getContent().stream().map(Product::getId).toList();
        java.util.List<com.campuscart.product.domain.Product> enriched = ids.isEmpty()
            ? java.util.List.of()
            : productRepository.findAllWithAssociationsByIdIn(ids);
        java.util.Map<java.util.UUID, com.campuscart.product.domain.Product> enrichedById = enriched.stream()
            .collect(java.util.stream.Collectors.toMap(com.campuscart.product.domain.Product::getId, p -> p));

        java.util.List<com.campuscart.product.domain.ProductImage> images = ids.isEmpty()
            ? java.util.List.of()
            : productImageRepository.findByProductIdInOrderByProductIdAscCreatedAtAsc(ids);
        java.util.Map<java.util.UUID, java.util.List<com.campuscart.product.domain.ProductImage>> imagesByProduct = images.stream()
            .collect(java.util.stream.Collectors.groupingBy(i -> i.getProduct().getId(), java.util.stream.Collectors.toList()));

        Page<ProductResponse> mapped = page.map(product -> {
            com.campuscart.product.domain.Product enrichedProduct = enrichedById.getOrDefault(product.getId(), product);
            java.util.List<com.campuscart.product.domain.ProductImage> imgs = imagesByProduct.getOrDefault(product.getId(), java.util.List.of());
            return productMapper.toResponse(enrichedProduct, imgs);
        });
        return PageResponse.from(mapped);
    }

    @Transactional
    public ProductResponse delete(UUID principalId, UUID productId) {
        Product product = requireWritableProduct(principalId, productId);
        product.softDelete();
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse markSold(UUID principalId, UUID productId) {
        Product product = requireWritableProduct(principalId, productId);
        if (product.getStatus() == ProductStatus.DELETED) {
            throw new BusinessRuleException("A deleted product cannot be marked sold.");
        }
        product.markSold();
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse activate(UUID principalId, UUID productId) {
        Product product = requireWritableProduct(principalId, productId);
        try {
            product.activate();
        } catch (IllegalStateException ex) {
            throw new BusinessRuleException(ex.getMessage());
        }
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse deactivate(UUID principalId, UUID productId) {
        Product product = requireWritableProduct(principalId, productId);
        try {
            product.deactivate();
        } catch (IllegalStateException ex) {
            throw new BusinessRuleException(ex.getMessage());
        }
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public Product requireWritableProduct(UUID principalId, UUID productId) {
        User principal = requireActiveUser(principalId);
        Product product = requireProduct(productId);
        if (!principal.getRole().isAdmin() && !product.getSeller().getId().equals(principalId)) {
            throw new AccessDeniedException("Product ownership is required for this action.");
        }
        return product;
    }

    @Transactional(readOnly = true)
    public Product requireDiscoverable(UUID principalId, UUID productId) {
        User viewer = requireActiveUser(principalId);
        Product product = requireProduct(productId);
        if (!canDiscover(product, viewer, MarketplaceScope.ALL_PRODUCTS)) {
            throw ResourceNotFoundException.of("Product", productId);
        }
        return product;
    }

    private User requireActiveUser(UUID principalId) {
        User user = userRepository.findById(principalId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", principalId));
        if (!user.getStatus().canAuthenticate()) {
            throw new AccountNotActiveException();
        }
        return user;
    }

    private Product requireProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));
    }

    private Category requireCategory(UUID categoryId) {
        return categoryRepository.findByIdAndActiveTrue(categoryId)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", categoryId));
    }

    private void validateReach(User seller, SellingReach reach) {
        if (reach == SellingReach.MY_CAMPUS && seller.getCollege() == null) {
            throw new BusinessRuleException("Community sellers cannot use MY_CAMPUS reach.");
        }
    }

    private boolean canDiscover(Product product, User viewer, MarketplaceScope scope) {
        if (viewer.getRole().isAdmin() || product.getSeller().getId().equals(viewer.getId())) {
            return true;
        }
        if (product.getStatus() != ProductStatus.ACTIVE) {
            return false;
        }
        boolean sameCity = product.getCity().getId().equals(viewer.getCity().getId());
        boolean sameCollege = viewer.getCollege() != null && product.getCollege() != null
                && product.getCollege().getId().equals(viewer.getCollege().getId());
        return switch (scope) {
            case MY_COLLEGE -> sameCollege;
            case NEARBY_COLLEGES -> sameCity && (product.getSellingReach() == SellingReach.OTHER_COLLEGES
                    || product.getSellingReach() == SellingReach.PUBLIC);
            case COMMUNITY_MARKETPLACE -> product.getSeller().getAccountType() == com.campuscart.user.domain.UserType.COMMUNITY
                    && (product.getSellingReach() == SellingReach.PUBLIC
                    || (sameCity && product.getSellingReach() == SellingReach.OTHER_COLLEGES));
            case ALL_PRODUCTS -> product.getSellingReach() == SellingReach.PUBLIC
                    || (sameCity && product.getSellingReach() == SellingReach.OTHER_COLLEGES)
                    || (sameCollege && product.getSellingReach() == SellingReach.MY_CAMPUS);
        };
    }

    private void validateQuery(ProductSearchQuery query) {
        if (query.page() < 0 || query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
            throw new BusinessRuleException("Page must be non-negative and size must be between 1 and 50.");
        }
        if (query.minPrice() != null && query.maxPrice() != null
                && query.minPrice().compareTo(query.maxPrice()) > 0) {
            throw new BusinessRuleException("Minimum price cannot exceed maximum price.");
        }
    }

    private Sort parseSort(String value) {
        String sort = value == null || value.isBlank() ? "createdAt,desc" : value.trim();
        String[] parts = sort.split(",", -1);
        String property = switch (parts[0]) {
            case "createdAt", "updatedAt", "price", "title", "quantity" -> parts[0];
            default -> throw new BusinessRuleException("Unsupported product sort field.");
        };
        Sort.Direction direction = parts.length > 1 ? Sort.Direction.fromOptionalString(parts[1]).orElse(null)
                : Sort.Direction.DESC;
        if (direction == null) {
            throw new BusinessRuleException("Unsupported product sort direction.");
        }
        return Sort.by(direction, property);
    }
}
