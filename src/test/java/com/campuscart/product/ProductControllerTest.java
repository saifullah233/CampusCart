package com.campuscart.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuscart.product.domain.ProductStatus;
import com.campuscart.product.domain.ProductType;
import com.campuscart.product.domain.SellingReach;
import com.campuscart.product.dto.CreateProductRequest;
import com.campuscart.product.dto.ProductImageResponse;
import com.campuscart.product.dto.ProductResponse;
import com.campuscart.product.service.ProductImageService;
import com.campuscart.product.service.ProductService;
import com.campuscart.product.web.ProductController;
import com.campuscart.security.AuthenticatedUser;
import com.campuscart.user.domain.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductImageService imageService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();
    private AuthenticatedUser principal;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        principal = new AuthenticatedUser(userId, "seller@college.edu", Role.STUDENT);

        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().equals(AuthenticatedUser.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return principal;
                    }
                })
                .build();
    }

    @Test
    void createJsonProduct_callsProductServiceWithNullImages() throws Exception {
        UUID categoryId = UUID.randomUUID();
        CreateProductRequest request = new CreateProductRequest(categoryId, "Laptop Stand",
                "Ergonomic aluminum stand", new BigDecimal("499.00"), ProductType.NEW,
                SellingReach.CAMPUS_ONLY, 1);

        ProductResponse response = new ProductResponse(
                UUID.randomUUID(), userId, "Student Seller", UUID.randomUUID(), "Campus",
                UUID.randomUUID(), "City", categoryId, "Electronics", "electronics",
                request.title(), request.description(), request.price(), request.productType(),
                request.sellingReach(), 1, ProductStatus.ACTIVE, List.of(), Instant.now(),
                Instant.now(), 0L);

        when(productService.create(eq(userId), eq(request), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Laptop Stand"));

        verify(productService).create(eq(userId), eq(request), any());
    }

    @Test
    void createMultipartProduct_callsProductServiceWithImages() throws Exception {
        UUID categoryId = UUID.randomUUID();
        MockMultipartFile image1 = new MockMultipartFile("images", "cover.jpg", "image/jpeg", new byte[]{1, 2, 3});
        MockMultipartFile image2 = new MockMultipartFile("images", "angle.png", "image/png", new byte[]{4, 5, 6});

        ProductImageResponse img1 = new ProductImageResponse(UUID.randomUUID(), "https://cdn/cover.jpg",
                "https://cdn/cover.jpg", "key-1", "image/jpeg", 3, 0, true, Instant.now());
        ProductImageResponse img2 = new ProductImageResponse(UUID.randomUUID(), "https://cdn/angle.png",
                "https://cdn/angle.png", "key-2", "image/png", 3, 1, false, Instant.now());

        ProductResponse response = new ProductResponse(
                UUID.randomUUID(), userId, "Student Seller", UUID.randomUUID(), "Campus",
                UUID.randomUUID(), "City", categoryId, "Electronics", "electronics",
                "Desk Lamp", "LED lamp", new BigDecimal("350.00"), ProductType.NEW,
                SellingReach.CAMPUS_ONLY, 1, ProductStatus.ACTIVE, List.of(img1, img2), Instant.now(),
                Instant.now(), 0L);

        when(productService.create(eq(userId), any(CreateProductRequest.class), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/products")
                        .file(image1)
                        .file(image2)
                        .param("title", "Desk Lamp")
                        .param("categoryId", categoryId.toString())
                        .param("description", "LED lamp")
                        .param("price", "350.00")
                        .param("productType", "NEW")
                        .param("sellingReach", "CAMPUS_ONLY")
                        .param("quantity", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.images.length()").value(2))
                .andExpect(jsonPath("$.data.images[0].isCover").value(true))
                .andExpect(jsonPath("$.data.images[1].isCover").value(false));
    }
}
