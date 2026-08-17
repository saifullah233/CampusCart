package com.campuscart.college.dto;

import java.util.UUID;

public record CollegeDetectionResponse(
        UUID collegeId,
        String collegeName,
        UUID cityId,
        String cityName) {
}
