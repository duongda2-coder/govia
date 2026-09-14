package com.govia.audit.khkt.scale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AuditKhktScaleRequest(
        @NotNull Integer sortOrder,
        BigDecimal creditThreshold,
        BigDecimal fundingThreshold,
        @NotBlank String scaleName,
        boolean active
) {
}
