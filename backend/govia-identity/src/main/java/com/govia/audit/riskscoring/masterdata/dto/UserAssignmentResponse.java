package com.govia.audit.riskscoring.masterdata.dto;

import java.util.UUID;

public record UserAssignmentResponse(
        UUID id,
        String username,
        UUID criteriaId,
        String criteriaCode,
        String branchCode,
        String classification,
        boolean active
) {
}
