package com.campuscart.security.otp;

import com.campuscart.common.exception.ApiException;
import com.campuscart.common.exception.ErrorCode;

public class OtpAttemptsExceededException extends ApiException {

    public OtpAttemptsExceededException() {
        super(ErrorCode.OTP_ATTEMPTS_EXCEEDED, "Too many verification attempts. Request a new code.");
    }
}
