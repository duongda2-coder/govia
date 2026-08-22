package com.govia.identity.dto;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        String username,
        String employeeCode,
        UUID tenantId,
        List<String> roles,
        List<String> permissions
) {
}
