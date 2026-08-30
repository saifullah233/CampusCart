package com.campuscart.auth.web;

import com.campuscart.auth.dto.ForgotPasswordRequest;
import com.campuscart.auth.dto.ForgotPasswordResponse;
import com.campuscart.auth.dto.PasswordResetVerificationResponse;
import com.campuscart.auth.dto.ResendOtpRequest;
import com.campuscart.auth.dto.ResetPasswordRequest;
import com.campuscart.auth.dto.VerifyPasswordResetOtpRequest;
import com.campuscart.auth.service.PasswordResetService;
import com.campuscart.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/forgot-password")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping
    public ApiResponse<ForgotPasswordResponse> requestPasswordReset(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ApiResponse.ok(
                "If an account exists with this email, a verification code has been sent.",
                passwordResetService.requestPasswordReset(request));
    }

    @PostMapping("/verify-otp")
    public ApiResponse<PasswordResetVerificationResponse> verifyOtp(
            @Valid @RequestBody VerifyPasswordResetOtpRequest request) {
        return ApiResponse.ok(
                "Verification code verified successfully.",
                passwordResetService.verifyOtp(request));
    }

    @PostMapping("/reset")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ApiResponse.ok(
                "Password reset successfully. Please log in with your new password.",
                null);
    }

    @PostMapping("/resend-otp")
    public ApiResponse<ForgotPasswordResponse> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {
        return ApiResponse.ok(
                "Verification code resent.",
                passwordResetService.resendOtp(request.challengeId()));
    }
}
