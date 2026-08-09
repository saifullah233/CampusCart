package com.campuscart.common.exception;

public class ImageLimitExceededException extends ApiException {

    public ImageLimitExceededException() {
        super(ErrorCode.IMAGE_LIMIT_EXCEEDED, "A product can have at most 8 images.");
    }
}
