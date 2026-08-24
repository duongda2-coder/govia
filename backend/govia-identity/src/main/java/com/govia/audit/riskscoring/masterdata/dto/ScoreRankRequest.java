package com.govia.audit.riskscoring.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ScoreRankRequest(
        @NotNull BigDecimal scoreFrom,
        @NotNull BigDecimal scoreTo,
        @NotBlank String rankLabel,
        @NotNull Integer fromYear,
        Integer toYear,
        boolean active
) {
}
