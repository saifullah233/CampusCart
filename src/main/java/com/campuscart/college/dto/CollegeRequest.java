package com.campuscart.college.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CollegeRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull UUID cityId) {
}
