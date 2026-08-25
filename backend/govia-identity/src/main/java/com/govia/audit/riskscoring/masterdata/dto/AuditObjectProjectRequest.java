package com.govia.audit.riskscoring.masterdata.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record AuditObjectProjectRequest(
        @NotBlank String code,
        @NotBlank String name,
        String projectType,
        String approvalAuthority,
        String purpose,
        BigDecimal investmentValue,
        String provider,
        String relatedParties,
        Integer inspectionYear,
        String inspectionResult,
        String inspectionRecommendation,
        Integer auditYear,
        String auditResult,
        String auditRecommendation,
        boolean active
) {
}
