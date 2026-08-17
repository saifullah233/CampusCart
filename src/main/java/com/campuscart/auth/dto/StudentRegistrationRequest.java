package com.campuscart.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StudentRegistrationRequest(
        @NotNull UUID cityId,
        @NotNull UUID collegeId,
        @NotBlank @Email @Size(max = 255) String officialEmail,
        @NotBlank @Size(max = 150) String fullName,
        @Size(max = 32) String phoneNumber,
        @NotBlank @Size(min = 8, max = 72) String password) {

    public StudentRegistrationRequest(UUID cityId, UUID collegeId, String officialEmail, String fullName, String password) {
        this(cityId, collegeId, officialEmail, fullName, null, password);
    }
}
