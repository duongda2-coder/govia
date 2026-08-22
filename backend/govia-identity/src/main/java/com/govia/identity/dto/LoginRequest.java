package com.govia.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String tenantCode,
        @NotBlank String username,
        @NotBlank String password
) {
}
