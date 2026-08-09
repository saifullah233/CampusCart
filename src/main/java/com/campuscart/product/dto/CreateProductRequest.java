package com.campuscart.product.dto;

import com.campuscart.product.domain.ProductType;
import com.campuscart.product.domain.SellingReach;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 10000) String description,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @NotNull ProductType productType,
        @NotNull SellingReach sellingReach,
        @jakarta.validation.constraints.Min(1) Integer quantity
) {
}
