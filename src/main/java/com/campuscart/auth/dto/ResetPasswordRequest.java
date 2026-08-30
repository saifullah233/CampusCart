package com.campuscart.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String resetToken,
        @NotBlank @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters") String newPassword) {
}
