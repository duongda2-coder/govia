package com.govia.audit.riskscoring.masterdata.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AuditObjectProjectResponse(
        UUID id,
        String code,
        String name,
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
