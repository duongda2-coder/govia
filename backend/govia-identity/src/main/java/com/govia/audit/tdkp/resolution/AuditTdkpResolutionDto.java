package com.govia.audit.tdkp.resolution;

import com.govia.audit.tdkp.common.TdkpStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public final class AuditTdkpResolutionDto {

    private AuditTdkpResolutionDto() {
    }

    /** Mã quản lý KHÔNG có trong request - hệ thống tự sinh. */
    public record Request(
            @NotBlank @Size(max = 20) String resolutionNumber,
            LocalDate issueDate,
            @Size(max = 500) String content,
            @Size(max = 500) String implementation,
            TdkpStatus status,
            @Size(max = 50) String supervisor,
            @Size(max = 500) String issuanceEvaluation,
            @Size(max = 500) String note
    ) {
    }

    public record Response(
            UUID id,
            String code,
            String resolutionNumber,
            LocalDate issueDate,
            String content,
            String implementation,
            TdkpStatus status,
            String statusLabel,
            String supervisor,
            String issuanceEvaluation,
            String note
    ) {
    }
}
