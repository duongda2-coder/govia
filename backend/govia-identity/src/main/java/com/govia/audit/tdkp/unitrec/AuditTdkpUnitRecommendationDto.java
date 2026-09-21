package com.govia.audit.tdkp.unitrec;

import com.govia.audit.tdkp.common.TdkpStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public final class AuditTdkpUnitRecommendationDto {

    private AuditTdkpUnitRecommendationDto() {
    }

    /** Mã kiến nghị KHÔNG có trong request - hệ thống tự sinh. */
    public record Request(
            @Size(max = 20) String reportNumber,
            LocalDate reportDate,
            UUID unitId,
            @NotBlank @Size(max = 2000) String content,
            LocalDate deadline,
            @Size(max = 500) String implementation,
            TdkpStatus status,
            @Size(max = 500) String evaluation,
            @Size(max = 500) String note
    ) {
    }

    public record Response(
            UUID id,
            String code,
            String reportNumber,
            LocalDate reportDate,
            UUID unitId,
            String unitCode,
            String unitName,
            String content,
            LocalDate deadline,
            String implementation,
            TdkpStatus status,
            String statusLabel,
            String evaluation,
            String deadlineState,
            String deadlineStateLabel,
            String note
    ) {
    }
}
