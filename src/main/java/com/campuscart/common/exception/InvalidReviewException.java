package com.campuscart.common.exception;

public class InvalidReviewException extends ApiException {

    public InvalidReviewException(String message) {
        super(ErrorCode.INVALID_REVIEW, message);
    }
}
