package com.govia.identity.activitylog.dto;

import com.govia.core.audit.AuditAction;

import java.time.Instant;
import java.util.UUID;

public record ActivityLogResponse(
        UUID id,
        String entityName,
        UUID entityId,
        AuditAction action,
        String detail,
        String performedBy,
        Instant performedAt
) {
}
