package com.campuscart.product;

import com.campuscart.product.domain.Product;
import com.campuscart.product.dto.ProductSearchQuery;
import com.campuscart.product.repository.ProductImageRepository;
import com.campuscart.product.repository.ProductRepository;
import com.campuscart.product.service.ProductMapper;
import com.campuscart.product.service.ProductService;
import com.campuscart.user.domain.User;
import com.campuscart.catalog.repository.CategoryRepository;
import com.campuscart.user.repository.UserRepository;
import com.campuscart.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceSearchBulkLoadTest {

    @Mock
    ProductRepository productRepository;
    @Mock
    ProductImageRepository productImageRepository;
    @Mock
    ProductMapper productMapper;
    @Mock
    CategoryRepository categoryRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    NotificationService notificationService;

    ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, categoryRepository, userRepository,
                productMapper, productImageRepository, notificationService);
        com.campuscart.user.domain.User user = mock(com.campuscart.user.domain.User.class);
        com.campuscart.user.domain.AccountStatus status = mock(com.campuscart.user.domain.AccountStatus.class);
        // AccountStatus is an enum; mock behaviour by stubbing the user's getStatus to a real ACTIVE value
        when(user.getStatus()).thenReturn(com.campuscart.user.domain.AccountStatus.ACTIVE);
        when(userRepository.findById(any())).thenReturn(java.util.Optional.of(user));
    }

    @Test
    void search_uses_bulk_fetch_for_associations_and_images() {
        Product p1 = mock(Product.class);
        UUID id1 = UUID.randomUUID();
        when(p1.getId()).thenReturn(id1);
        var page = new PageImpl<Product>(List.of(p1), PageRequest.of(0, 10), 1);

        when(productRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Product>>any(), ArgumentMatchers.any(PageRequest.class))).thenReturn(page);
        when(productRepository.findAllWithAssociationsByIdIn(List.of(id1))).thenReturn(List.of(p1));
        when(productImageRepository.findByProductIdInOrderByProductIdAscCreatedAtAsc(List.of(id1))).thenReturn(List.of());

        productService.search(UUID.randomUUID(), new com.campuscart.product.dto.ProductSearchQuery(
            null, null, null, null, null, null, null, null, null, null, 0, 10, null));

        verify(productRepository).findAllWithAssociationsByIdIn(List.of(id1));
        verify(productImageRepository).findByProductIdInOrderByProductIdAscCreatedAtAsc(List.of(id1));
    }
}
