package com.govia.audit.riskscoring.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuditObjectCategoryRequest(
        @NotBlank @Size(max = 4) String code,
        @NotBlank @Size(max = 50) String name,
        @Size(max = 200) String note,
        boolean active
) {
}
