package com.govia.audit.riskscoring.scoring.dto;

import java.util.UUID;

public record GroupHOResponse(
        UUID id,
        String code,
        String name,
        String note,
        boolean active
) {
}
