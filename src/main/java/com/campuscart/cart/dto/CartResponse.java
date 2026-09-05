package com.campuscart.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        List<CartItemResponse> items,
        BigDecimal total,
        boolean checkoutAvailable) {

    @JsonProperty("totalAmount")
    public BigDecimal totalAmount() {
        return total;
    }

    @JsonProperty("checkoutReady")
    public boolean checkoutReady() {
        return checkoutAvailable;
    }
}

