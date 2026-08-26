package com.campuscart.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.campuscart.catalog.domain.Category;
import com.campuscart.catalog.repository.CategoryRepository;
import com.campuscart.college.domain.College;
import com.campuscart.common.exception.ImageLimitExceededException;
import com.campuscart.location.domain.City;
import com.campuscart.notification.service.NotificationService;
import com.campuscart.product.domain.Product;
import com.campuscart.product.domain.ProductImage;
import com.campuscart.product.domain.ProductType;
import com.campuscart.product.domain.SellingReach;
import com.campuscart.product.dto.CreateProductRequest;
import com.campuscart.product.dto.ProductResponse;
import com.campuscart.product.image.ImageFileValidator;
import com.campuscart.product.image.ProductImageStorage;
import com.campuscart.product.repository.ProductImageRepository;
import com.campuscart.product.repository.ProductRepository;
import com.campuscart.product.service.ProductMapper;
import com.campuscart.product.service.ProductService;
import com.campuscart.user.domain.AccountStatus;
import com.campuscart.user.domain.User;
import com.campuscart.user.repository.UserRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ProductServiceImageCreationTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductImageRepository productImageRepository;
    @Mock
    private ProductImageStorage imageStorage;
    @Mock
    private NotificationService notificationService;

    private ImageFileValidator imageFileValidator = new ImageFileValidator();
    private ProductService productService;

    private User seller;
    private Category category;
    private UUID principalId;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, categoryRepository, userRepository,
                productMapper, productImageRepository, imageStorage, imageFileValidator, notificationService);

        principalId = UUID.randomUUID();
        City city = new City("Delhi", "Delhi");
        College college = new College("Engineering College", city);
        seller = new User("seller@college.edu", "Seller Name", college);
        seller.activateAfterEmailVerification();

        category = new Category("Electronics", "electronics");

        when(userRepository.findById(principalId)).thenReturn(Optional.of(seller));
        when(categoryRepository.findByIdAndActiveTrue(category.getId())).thenReturn(Optional.of(category));
    }

    @Test
    void createProductWithoutImages_success() {
        CreateProductRequest request = new CreateProductRequest(category.getId(), "Scientific Calculator",
                "Like new FX-991EX", new BigDecimal("750.00"), ProductType.NEW, SellingReach.CAMPUS_ONLY, 1);

        Product savedProduct = new Product(seller, seller.getCollege(), seller.getCity(), category,
                request.title(), request.description(), request.price(), request.productType(),
                request.sellingReach(), 1);

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        productService.create(principalId, request, null);

        verify(productRepository).save(any(Product.class));
        verifyNoInteractions(imageStorage);
        verify(productImageRepository, never()).save(any(ProductImage.class));
    }

    @Test
    void createProductWithImages_setsCoverAndDisplayOrderCorrectly() throws Exception {
        CreateProductRequest request = new CreateProductRequest(category.getId(), "Lab Kit",
                "Complete chemistry set", new BigDecimal("500.00"), ProductType.NEW, SellingReach.CAMPUS_ONLY, 1);

        Product savedProduct = new Product(seller, seller.getCollege(), seller.getCity(), category,
                request.title(), request.description(), request.price(), request.productType(),
                request.sellingReach(), 1);

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        MockMultipartFile file1 = createMockImage("photo1.jpg");
        MockMultipartFile file2 = createMockImage("photo2.png");
        MockMultipartFile file3 = createMockImage("photo3.jpg");

        when(imageStorage.store(eq(savedProduct.getId()), any(MultipartFile.class)))
                .thenAnswer(invocation -> {
                    MultipartFile f = invocation.getArgument(1);
                    return new ProductImageStorage.StoredImage("key-" + f.getOriginalFilename(),
                            "https://cdn.example.com/" + f.getOriginalFilename(),
                            f.getContentType(), f.getSize());
                });

        productService.create(principalId, request, List.of(file1, file2, file3));

        ArgumentCaptor<ProductImage> imageCaptor = ArgumentCaptor.forClass(ProductImage.class);
        verify(productImageRepository, times(3)).save(imageCaptor.capture());

        List<ProductImage> savedImages = imageCaptor.getAllValues();
        assertThat(savedImages).hasSize(3);

        // Image 0 -> cover = true, displayOrder = 0
        assertThat(savedImages.get(0).isCover()).isTrue();
        assertThat(savedImages.get(0).getDisplayOrder()).isEqualTo(0);
        assertThat(savedImages.get(0).getDeliveryUrl()).isEqualTo("https://cdn.example.com/photo1.jpg");

        // Image 1 -> cover = false, displayOrder = 1
        assertThat(savedImages.get(1).isCover()).isFalse();
        assertThat(savedImages.get(1).getDisplayOrder()).isEqualTo(1);

        // Image 2 -> cover = false, displayOrder = 2
        assertThat(savedImages.get(2).isCover()).isFalse();
        assertThat(savedImages.get(2).getDisplayOrder()).isEqualTo(2);
    }

    @Test
    void createProductWithSixImages_rejected() throws Exception {
        CreateProductRequest request = new CreateProductRequest(category.getId(), "Too Many",
                "Lots of photos", new BigDecimal("100.00"), ProductType.NEW, SellingReach.CAMPUS_ONLY, 1);

        List<MultipartFile> sixImages = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            sixImages.add(createMockImage("photo" + i + ".jpg"));
        }

        assertThatThrownBy(() -> productService.create(principalId, request, sixImages))
                .isInstanceOf(ImageLimitExceededException.class);

        verifyNoInteractions(productRepository);
        verifyNoInteractions(imageStorage);
    }

    @Test
    void createProductWithStorageFailure_cleansUpPreviouslyUploadedImages() throws Exception {
        CreateProductRequest request = new CreateProductRequest(category.getId(), "Kit",
                "Description", new BigDecimal("100.00"), ProductType.NEW, SellingReach.CAMPUS_ONLY, 1);

        Product savedProduct = new Product(seller, seller.getCollege(), seller.getCity(), category,
                request.title(), request.description(), request.price(), request.productType(),
                request.sellingReach(), 1);

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        MockMultipartFile file1 = createMockImage("photo1.jpg");
        MockMultipartFile file2 = createMockImage("photo2.jpg");

        when(imageStorage.store(eq(savedProduct.getId()), eq(file1)))
                .thenReturn(new ProductImageStorage.StoredImage("key-photo1", "https://cdn/1.jpg", "image/jpeg", 100));
        when(imageStorage.store(eq(savedProduct.getId()), eq(file2)))
                .thenThrow(new RuntimeException("Cloudinary network timeout"));

        assertThatThrownBy(() -> productService.create(principalId, request, List.of(file1, file2)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cloudinary network timeout");

        // Verify key-photo1 was cleaned up from Cloudinary
        verify(imageStorage).delete("key-photo1");
    }

    private MockMultipartFile createMockImage(String name) throws Exception {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String format = name.endsWith(".png") ? "png" : "jpg";
        String mime = name.endsWith(".png") ? "image/png" : "image/jpeg";
        ImageIO.write(img, format, baos);
        return new MockMultipartFile("images", name, mime, baos.toByteArray());
    }
}
