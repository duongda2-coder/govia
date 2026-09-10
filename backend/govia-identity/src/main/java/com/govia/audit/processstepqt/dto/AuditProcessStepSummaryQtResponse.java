package com.govia.audit.processstepqt.dto;

import java.util.UUID;

public record AuditProcessStepSummaryQtResponse(
        UUID id,
        UUID businessSegmentId,
        String businessSegmentCode,
        String businessSegmentName,
        String code,
        String name,
        UUID workItemId,
        String workItemCode,
        String workItemName,
        Integer applicableYear,
        boolean active
) {
}
