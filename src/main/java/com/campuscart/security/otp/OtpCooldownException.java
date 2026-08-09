package com.campuscart.security.otp;

import com.campuscart.common.exception.ApiException;
import com.campuscart.common.exception.ErrorCode;

public class OtpCooldownException extends ApiException {

    public OtpCooldownException() {
        super(ErrorCode.OTP_COOLDOWN, "Please wait before requesting another verification code.");
    }
}
