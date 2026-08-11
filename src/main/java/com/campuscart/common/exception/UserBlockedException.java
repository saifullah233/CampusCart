package com.campuscart.common.exception;

public class UserBlockedException extends ApiException {

    public UserBlockedException() {
        super(ErrorCode.USER_BLOCKED, "Messaging is unavailable between these users.");
    }
}
