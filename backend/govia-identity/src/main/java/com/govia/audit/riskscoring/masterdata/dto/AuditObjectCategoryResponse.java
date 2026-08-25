package com.govia.audit.riskscoring.masterdata.dto;

import java.util.UUID;

public record AuditObjectCategoryResponse(
        UUID id,
        String code,
        String name,
        String note,
        boolean active
) {
}
