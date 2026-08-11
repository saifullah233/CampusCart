package com.campuscart.common.exception;

public class LoginRateLimitedException extends ApiException {

    public LoginRateLimitedException() {
        super(ErrorCode.LOGIN_RATE_LIMITED, "Too many login attempts. Try again later.");
    }
}
