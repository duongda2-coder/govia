package com.govia.audit.riskscoring.scoring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RiskBranchScoreExpertRankResponse(
        UUID id,
        Integer year,
        String branchCode,
        String branchName,
        BigDecimal totalScore,
        String baseRankLabel,
        String reRankLabel,
        String reason,
        LocalDate assessedDate,
        String expertName,
        String finalRankLabel,
        String updatedBy
) {
}
