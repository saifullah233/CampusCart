package com.campuscart.common.exception;

public class ProductUnavailableException extends ApiException {

    public ProductUnavailableException(String message) {
        super(ErrorCode.PRODUCT_UNAVAILABLE, message);
    }
}
