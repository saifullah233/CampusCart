package com.campuscart.security.otp;

import com.campuscart.common.exception.ApiException;
import com.campuscart.common.exception.ErrorCode;

public class OtpRateLimitedException extends ApiException {

    public OtpRateLimitedException() {
        super(ErrorCode.OTP_RATE_LIMITED, "Too many verification codes requested. Try again later.");
    }
}
