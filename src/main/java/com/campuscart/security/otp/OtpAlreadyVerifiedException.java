package com.campuscart.security.otp;

import com.campuscart.common.exception.ApiException;
import com.campuscart.common.exception.ErrorCode;

public class OtpAlreadyVerifiedException extends ApiException {

    public OtpAlreadyVerifiedException() {
        super(ErrorCode.OTP_ALREADY_VERIFIED, "This verification challenge has already been completed.");
    }
}
