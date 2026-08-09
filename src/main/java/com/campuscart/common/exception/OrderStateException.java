package com.campuscart.common.exception;

public class OrderStateException extends ApiException {

    public OrderStateException(String message) {
        super(ErrorCode.ORDER_STATE_INVALID, message);
    }
}
