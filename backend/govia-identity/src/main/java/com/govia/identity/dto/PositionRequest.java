package com.govia.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record PositionRequest(
        @NotBlank String code,
        @NotBlank String name
) {
}
