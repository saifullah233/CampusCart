package com.campuscart.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<CartItemResponse> items, BigDecimal total, boolean checkoutAvailable) {
}
