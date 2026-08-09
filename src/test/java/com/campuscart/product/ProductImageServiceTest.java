package com.campuscart.product;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campuscart.common.exception.ImageLimitExceededException;
import com.campuscart.product.domain.Product;
import com.campuscart.product.image.ImageFileValidator;
import com.campuscart.product.image.ProductImageStorage;
import com.campuscart.product.repository.ProductImageRepository;
import com.campuscart.product.service.ProductImageService;
import com.campuscart.product.service.ProductService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductImageRepository imageRepository;

    @Mock
    private ProductImageStorage imageStorage;

    private ProductImageService imageService;

    @BeforeEach
    void setUp() {
        imageService = new ProductImageService(productService, imageRepository, imageStorage,
                new ImageFileValidator());
    }

    @Test
    void ownershipIsCheckedBeforeUpload() {
        UUID principalId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(productService.requireWritableProduct(principalId, productId))
                .thenThrow(new AccessDeniedException("denied"));

        assertThatThrownBy(() -> imageService.add(principalId, productId,
                new MockMultipartFile("file", "x.jpg", "image/jpeg", new byte[]{1, 2, 3})))
                .isInstanceOf(AccessDeniedException.class);
        verify(imageStorage, never()).store(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void imageLimitIsCheckedBeforeStorage() {
        UUID principalId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(productService.requireWritableProduct(principalId, productId)).thenReturn(mock(Product.class));
        when(imageRepository.countByProductId(productId)).thenReturn(8L);

        assertThatThrownBy(() -> imageService.add(principalId, productId,
                new MockMultipartFile("file", "x.jpg", "image/jpeg",
                        new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff})))
                .isInstanceOf(ImageLimitExceededException.class);
        verify(imageStorage, never()).store(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
