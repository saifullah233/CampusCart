package com.campuscart.security.otp;

import com.campuscart.common.exception.ApiException;
import com.campuscart.common.exception.ErrorCode;

public class OtpInvalidException extends ApiException {

    public OtpInvalidException() {
        super(ErrorCode.OTP_INVALID, "The verification code is invalid or has expired.");
    }
}
