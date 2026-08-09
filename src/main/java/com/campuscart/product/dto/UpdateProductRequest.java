package com.campuscart.product.dto;

import com.campuscart.product.domain.ProductType;
import com.campuscart.product.domain.SellingReach;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductRequest(
        UUID categoryId,
        @Size(max = 180) String title,
        @Size(max = 10000) String description,
        @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
        ProductType productType,
        SellingReach sellingReach,
        @Min(1) Integer quantity
) {
}
