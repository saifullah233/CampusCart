package com.campuscart.common.exception;

public class InvalidRequestException extends ApiException {

    public InvalidRequestException(String message) {
        super(ErrorCode.MALFORMED_REQUEST, message);
    }
}
