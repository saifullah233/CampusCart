package com.campuscart.common.exception;

public class InvalidImageException extends ApiException {

    public InvalidImageException(String message) {
        super(ErrorCode.INVALID_IMAGE, message);
    }
}
