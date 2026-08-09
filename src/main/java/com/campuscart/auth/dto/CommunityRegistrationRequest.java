package com.campuscart.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CommunityRegistrationRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 150) String fullName,
        @NotNull UUID cityId,
        @NotBlank @Pattern(regexp = "^\\+?[0-9 ()-]{8,24}$") String phoneNumber,
        @NotBlank @Size(min = 8, max = 72) String password) {
}
