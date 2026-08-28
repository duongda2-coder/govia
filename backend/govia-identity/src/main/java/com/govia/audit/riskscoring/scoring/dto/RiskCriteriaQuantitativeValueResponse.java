package com.govia.audit.riskscoring.scoring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RiskCriteriaQuantitativeValueResponse(
        UUID id,
        Integer year,
        String branchCode,
        String branchName,
        UUID criteriaId,
        String criteriaCode,
        String criteriaName,
        LocalDate entryDate,
        BigDecimal value
) {
}
