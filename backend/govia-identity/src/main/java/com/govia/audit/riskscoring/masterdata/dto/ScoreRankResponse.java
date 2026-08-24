package com.govia.audit.riskscoring.masterdata.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ScoreRankResponse(
        UUID id,
        BigDecimal scoreFrom,
        BigDecimal scoreTo,
        String rankLabel,
        Integer fromYear,
        Integer toYear,
        boolean active
) {
}
