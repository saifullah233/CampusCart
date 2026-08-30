package com.campuscart.product.web;

import com.campuscart.common.api.ApiResponse;
import com.campuscart.common.api.PageResponse;
import com.campuscart.product.domain.MarketplaceScope;
import com.campuscart.product.domain.ProductStatus;
import com.campuscart.product.domain.ProductType;
import com.campuscart.product.domain.SellingReach;
import com.campuscart.product.dto.CreateProductRequest;
import com.campuscart.product.dto.ProductImageResponse;
import com.campuscart.product.dto.ProductResponse;
import com.campuscart.product.dto.ProductSearchQuery;
import com.campuscart.product.dto.UpdateProductRequest;
import com.campuscart.product.service.ProductImageService;
import com.campuscart.product.service.ProductService;
import com.campuscart.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;
    private final ProductImageService imageService;

    public ProductController(ProductService productService, ProductImageService imageService) {
        this.productService = productService;
        this.imageService = imageService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ProductResponse>> createJson(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product created.", productService.create(principal.id(), request, null)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductResponse>> createMultipart(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam("title") String title,
            @RequestParam("categoryId") UUID categoryId,
            @RequestParam("description") String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("productType") ProductType productType,
            @RequestParam("sellingReach") SellingReach sellingReach,
            @RequestParam(value = "quantity", required = false, defaultValue = "1") Integer quantity,
            @RequestPart(value = "images", required = false) java.util.List<MultipartFile> images) {
        CreateProductRequest request = new CreateProductRequest(categoryId, title, description, price, productType, sellingReach, quantity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product created.", productService.create(principal.id(), request, images)));
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> search(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) ProductType productType,
            @RequestParam(required = false) SellingReach sellingReach,
            @RequestParam(required = false) UUID collegeId,
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "ALL_PRODUCTS") MarketplaceScope scope,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        ProductSearchQuery query = new ProductSearchQuery(keyword, categoryId, productType, sellingReach,
                collegeId, cityId, minPrice, maxPrice, status, scope, page, size, sort);
        return ApiResponse.ok(productService.search(principal.id(), query));
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<ProductResponse>> myListings(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ApiResponse.ok(productService.findMyListings(principal.id(), status, page, size, sort));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> get(@AuthenticationPrincipal AuthenticatedUser principal,
                                             @PathVariable UUID id) {
        return ApiResponse.ok(productService.get(principal.id(), id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ProductResponse> update(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @PathVariable UUID id,
                                               @Valid @RequestBody UpdateProductRequest request) {
        return ApiResponse.ok(productService.update(principal.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<ProductResponse> delete(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @PathVariable UUID id) {
        return ApiResponse.ok("Product deleted.", productService.delete(principal.id(), id));
    }

    @PostMapping("/{id}/sold")
    public ApiResponse<ProductResponse> markSold(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @PathVariable UUID id) {
        return ApiResponse.ok("Product marked sold.", productService.markSold(principal.id(), id));
    }

    @PostMapping("/{id}/activate")
    public ApiResponse<ProductResponse> activate(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @PathVariable UUID id) {
        return ApiResponse.ok("Product activated.", productService.activate(principal.id(), id));
    }

    @PostMapping("/{id}/deactivate")
    public ApiResponse<ProductResponse> deactivate(@AuthenticationPrincipal AuthenticatedUser principal,
                                                   @PathVariable UUID id) {
        return ApiResponse.ok("Product deactivated.", productService.deactivate(principal.id(), id));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductImageResponse>> addImage(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product image uploaded.", imageService.add(principal.id(), id, file)));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ApiResponse<Void> deleteImage(@AuthenticationPrincipal AuthenticatedUser principal,
                                         @PathVariable UUID id, @PathVariable UUID imageId) {
        imageService.delete(principal.id(), id, imageId);
        return ApiResponse.ok("Product image deleted.", null);
    }
}
