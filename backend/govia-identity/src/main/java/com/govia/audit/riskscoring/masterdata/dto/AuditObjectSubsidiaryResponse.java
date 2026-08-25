package com.govia.audit.riskscoring.masterdata.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuditObjectSubsidiaryResponse(
        UUID id,
        String code,
        String name,
        String companyType,
        LocalDate establishedDate,
        Integer staffCount,
        Integer leaderCount,
        Integer inspectionYear,
        String inspectionResult,
        String inspectionRecommendation,
        Integer auditYear,
        String auditResult,
        String auditRecommendation,
        BigDecimal revenue,
        BigDecimal cost,
        BigDecimal profit,
        BigDecimal salaryFund,
        boolean active
) {
}
