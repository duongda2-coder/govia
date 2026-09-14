package com.govia.audit.khkt.scale.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AuditKhktScaleResponse(
        UUID id,
        Integer sortOrder,
        BigDecimal creditThreshold,
        BigDecimal fundingThreshold,
        String scaleName,
        boolean active
) {
}
