package com.govia.audit.riskscoring.scoring.dto;

import java.time.LocalDate;

public record RiskBranchScoreExpertRankRequest(
        String reRankLabel,
        String reason,
        LocalDate assessedDate,
        String expertName,
        String finalRankLabel
) {
}
