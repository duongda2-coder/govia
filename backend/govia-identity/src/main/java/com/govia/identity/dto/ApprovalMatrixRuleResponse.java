package com.govia.identity.dto;

import com.govia.identity.entity.EmployeeRankLevel;

import java.util.UUID;

public record ApprovalMatrixRuleResponse(
        UUID id,
        UUID orgUnitId,
        String orgUnitCode,
        String orgUnitName,
        EmployeeRankLevel finalApprovalLevel,
        boolean requireFinalSuperAdminStep,
        boolean active
) {
}
