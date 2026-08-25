package com.govia.audit.riskscoring.masterdata.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AuditObjectSubsidiaryRequest(
        @NotBlank String code,
        @NotBlank String name,
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
