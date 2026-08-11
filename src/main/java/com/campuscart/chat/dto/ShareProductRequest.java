package com.campuscart.chat.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ShareProductRequest(@NotNull UUID productId) {
}
