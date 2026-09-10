package com.govia.audit.workitemqt.dto;

import com.govia.audit.workitem.entity.AuditWorkPhase;

import java.util.UUID;

public record AuditWorkItemQtResponse(
        UUID id,
        UUID auditObjectCategoryId,
        String auditObjectCategoryCode,
        String auditObjectCategoryName,
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
        boolean active,
        boolean hasSampleSelection
) {
}
