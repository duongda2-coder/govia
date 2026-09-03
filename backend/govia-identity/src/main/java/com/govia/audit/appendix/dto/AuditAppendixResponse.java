package com.govia.audit.appendix.dto;

import java.util.UUID;

public record AuditAppendixResponse(
        UUID id,
        UUID businessSegmentId,
        String businessSegmentCode,
        String businessSegmentName,
        String sampleType,
        String appendixCode,
        String note,
        boolean active
) {
}
