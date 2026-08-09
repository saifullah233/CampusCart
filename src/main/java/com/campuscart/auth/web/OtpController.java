package com.campuscart.auth.web;

import com.campuscart.auth.dto.ResendOtpRequest;
import com.campuscart.auth.dto.VerificationResponse;
import com.campuscart.auth.dto.VerifyOtpRequest;
import com.campuscart.auth.service.AuthService;
import com.campuscart.auth.service.OtpChallengeResponse;
import com.campuscart.auth.service.OtpService;
import com.campuscart.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/otp")
public class OtpController {

    private final OtpService otpService;
    private final AuthService authService;

    public OtpController(OtpService otpService, AuthService authService) {
        this.otpService = otpService;
        this.authService = authService;
    }

    @PostMapping("/verify")
    public ApiResponse<VerificationResponse> verify(@Valid @RequestBody VerifyOtpRequest request) {
        return ApiResponse.ok(authService.verifyRegistration(request.challengeId(), request.code()));
    }

    @PostMapping("/resend")
    public ApiResponse<OtpChallengeResponse> resend(@Valid @RequestBody ResendOtpRequest request) {
        return ApiResponse.ok(otpService.resend(request.challengeId()));
    }
}
