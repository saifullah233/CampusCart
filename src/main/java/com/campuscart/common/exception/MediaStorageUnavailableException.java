package com.campuscart.common.exception;

public class MediaStorageUnavailableException extends ApiException {

    public MediaStorageUnavailableException() {
        super(ErrorCode.MEDIA_STORAGE_UNAVAILABLE, "Image storage is not configured.");
    }

    public MediaStorageUnavailableException(Throwable cause) {
        super(ErrorCode.MEDIA_STORAGE_UNAVAILABLE, "Image storage is temporarily unavailable.", cause);
    }
}
