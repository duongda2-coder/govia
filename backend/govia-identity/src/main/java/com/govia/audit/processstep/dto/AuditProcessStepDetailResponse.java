package com.govia.audit.processstep.dto;

import java.util.UUID;

public record AuditProcessStepDetailResponse(
        UUID id,
        UUID businessSegmentId,
        String businessSegmentCode,
        String businessSegmentName,
        UUID processStepSummaryId,
        String processStepSummaryCode,
        String processStepSummaryName,
        UUID controlPointId,
        String controlPointCode,
        String controlPointName,
        String code,
        boolean active
) {
}
