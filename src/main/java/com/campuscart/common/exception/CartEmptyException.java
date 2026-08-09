package com.campuscart.common.exception;

public class CartEmptyException extends ApiException {

    public CartEmptyException() {
        super(ErrorCode.CART_EMPTY, "Your cart is empty.");
    }
}
