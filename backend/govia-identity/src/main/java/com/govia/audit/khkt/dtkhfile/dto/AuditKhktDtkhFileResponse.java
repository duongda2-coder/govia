package com.govia.audit.khkt.dtkhfile.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditKhktDtkhFileResponse(
        UUID id,
        Integer year,
        String departmentCode,
        String versionCode,
        String uploadedByUsername,
        String uploadedByName,
        UUID attachmentId,
        String fileName,
        Instant uploadedAt,
        String note
) {
}
