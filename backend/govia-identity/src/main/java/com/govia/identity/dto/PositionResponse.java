package com.govia.identity.dto;

import java.util.UUID;

public record PositionResponse(
        UUID id,
        String code,
        String name,
        boolean active
) {
}
