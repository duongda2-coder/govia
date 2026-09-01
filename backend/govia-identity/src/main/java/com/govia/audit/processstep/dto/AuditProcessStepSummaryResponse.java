package com.govia.audit.processstep.dto;

import java.util.UUID;

public record AuditProcessStepSummaryResponse(
        UUID id,
        UUID businessSegmentId,
        String businessSegmentCode,
        String businessSegmentName,
        String code,
        String name,
        UUID workItemId,
        String workItemCode,
        String workItemName,
        boolean active
) {
}
