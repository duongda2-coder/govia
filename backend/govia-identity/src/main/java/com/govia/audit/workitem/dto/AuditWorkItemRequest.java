package com.govia.audit.workitem.dto;

import com.govia.audit.workitem.entity.AuditWorkPhase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AuditWorkItemRequest(
        AuditWorkPhase phase,
        UUID businessSegmentId,
        @NotBlank @Size(max = 10) String code,
        @NotBlank @Size(max = 1000) String name,
        Integer applicableYear,
        String workType,
        boolean active,
        boolean hasSampleSelection
) {
}
