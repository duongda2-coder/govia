package com.govia.identity.activitylog.dto;

import com.govia.core.audit.AuditAction;

import java.time.LocalDate;
import java.util.UUID;

/** Tieu chi loc/tim man hinh "Nhat ky thao tac" - dung chung cho list va export Excel/Word. */
public record ActivityLogFilter(
        String entityName,
        AuditAction action,
        String performedBy,
        UUID entityId,
        LocalDate dateFrom,
        LocalDate dateTo,
        String keyword
) {
}
