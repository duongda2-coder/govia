package com.govia.audit.workitem.dto;

import com.govia.audit.workitem.entity.AuditWorkPhase;

import java.util.UUID;

public record AuditWorkItemResponse(
        UUID id,
        AuditWorkPhase phase,
        UUID businessSegmentId,
        String businessSegmentCode,
        String businessSegmentName,
        String code,
        String name,
        Integer applicableYear,
        String workSetCode,
        String workType,
        boolean active
) {
}
