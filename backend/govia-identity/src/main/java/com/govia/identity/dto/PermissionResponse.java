package com.govia.identity.dto;

import java.util.UUID;

public record PermissionResponse(
        UUID id,
        String code,
        String module,
        String description,
        String resourceLabel
) {
}
