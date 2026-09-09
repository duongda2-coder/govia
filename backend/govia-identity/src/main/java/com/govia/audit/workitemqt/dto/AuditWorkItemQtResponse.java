package com.govia.audit.workitemqt.dto;

import com.govia.audit.workitem.entity.AuditWorkPhase;

import java.util.UUID;

public record AuditWorkItemQtResponse(
        UUID id,
        AuditWorkPhase phase,
        UUID businessSegmentId,
        String businessSegmentCode,
        String businessSegmentName,
        String code,
        String detailCode,
        String name,
        Integer applicableYear,
        String workSetCode,
        String workType,
        boolean active
) {
}
