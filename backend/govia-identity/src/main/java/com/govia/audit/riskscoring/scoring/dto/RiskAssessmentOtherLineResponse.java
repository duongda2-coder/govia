package com.govia.audit.riskscoring.scoring.dto;

import java.util.UUID;

public record RiskAssessmentOtherLineResponse(
        UUID id,
        UUID headerId,
        UUID criteriaOtherId,
        String criteriaOtherCode,
        String criteriaOtherName,
        UUID scaleId,
        Integer scaleScore,
        String ratingLevel
) {
}
