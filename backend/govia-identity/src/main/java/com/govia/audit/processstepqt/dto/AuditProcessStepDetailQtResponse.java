package com.govia.audit.processstepqt.dto;

import java.util.UUID;

public record AuditProcessStepDetailQtResponse(
        UUID id,
        UUID businessSegmentId,
        String businessSegmentCode,
        String businessSegmentName,
        UUID processStepSummaryId,
        String processStepSummaryCode,
        String processStepSummaryName,
        String code,
        Integer applicableYear,
        boolean active
) {
}
