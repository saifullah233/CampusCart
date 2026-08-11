package com.campuscart.common.exception;

public class UnsafeContentException extends ApiException {

    public UnsafeContentException(String message) {
        super(ErrorCode.UNSAFE_CONTENT, message);
    }
}
