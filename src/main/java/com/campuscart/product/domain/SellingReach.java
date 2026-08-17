package com.campuscart.product.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SellingReach {
    CAMPUS_ONLY,
    OUTSIDE_CAMPUS;

    @JsonCreator
    public static SellingReach fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (value.trim().toUpperCase()) {
            case "CAMPUS_ONLY", "MY_CAMPUS" -> CAMPUS_ONLY;
            case "OUTSIDE_CAMPUS", "PUBLIC", "OTHER_COLLEGES", "INTRA_CITY", "ALL_NCR" -> OUTSIDE_CAMPUS;
            default -> throw new IllegalArgumentException("Unknown SellingReach: " + value);
        };
    }
}
