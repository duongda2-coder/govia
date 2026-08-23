package com.govia.identity.dto;

import com.govia.identity.entity.EmployeeRankLevel;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** orgUnitId null = quy tac mac dinh (fallback) cho toan tenant. */
public record ApprovalMatrixRuleRequest(
        UUID orgUnitId,
        @NotNull EmployeeRankLevel finalApprovalLevel,
        boolean requireFinalSuperAdminStep,
        boolean active
) {
}
