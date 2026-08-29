package com.govia.audit.riskscoring.scoring.dto;

import java.util.UUID;

public record RiskCriteriaQualitativeValueResponse(
        UUID id,
        Integer year,
        String branchCode,
        String branchName,
        UUID criteriaId,
        String criteriaCode,
        String criteriaName,
        String group1Code,
        String group2Code,
        String violation,
        String note
) {
}
