package com.campuscart.common.exception;

public class ImageLimitExceededException extends ApiException {

    public ImageLimitExceededException() {
        this("You can add up to 5 photos.");
    }

    public ImageLimitExceededException(String message) {
        super(ErrorCode.IMAGE_LIMIT_EXCEEDED, message);
    }
}
