package com.govia.audit.khkt.dtkhfile.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AuditKhktDtkhFileUpdateRequest(
        @NotNull UUID departmentId,
        UUID versionId,
        String note
) {
}
