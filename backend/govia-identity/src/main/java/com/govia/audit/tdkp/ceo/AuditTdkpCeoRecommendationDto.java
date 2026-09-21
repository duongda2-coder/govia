package com.govia.audit.tdkp.ceo;

import com.govia.audit.tdkp.common.TdkpStatus;
import com.govia.audit.tdkp.common.TdkpTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public final class AuditTdkpCeoRecommendationDto {

    private AuditTdkpCeoRecommendationDto() {
    }

    public record Request(
            @Size(max = 50) String reportNumber,
            LocalDate reportDate,
            @NotBlank @Size(max = 2000) String content,
            UUID recommendationTypeId,
            UUID businessSegmentId,
            TdkpTarget targetObject,
            UUID executingUnitId,
            @Size(max = 500) String directive,
            LocalDate deadline,
            TdkpStatus status,
            @Size(max = 500) String evaluation,
            @Size(max = 500) String note
    ) {
    }

    public record Response(
            UUID id,
            TdkpCeoScope scope,
            UUID sourceId,
            String reportNumber,
            LocalDate reportDate,
            String content,
            UUID recommendationTypeId,
            String recommendationTypeName,
            UUID businessSegmentId,
            String businessSegmentCode,
            TdkpTarget targetObject,
            String targetObjectLabel,
            UUID executingUnitId,
            String executingUnitName,
            String directive,
            LocalDate deadline,
            TdkpStatus status,
            String statusLabel,
            String evaluation,
            /** ON_TIME / OVERDUE - tính theo ngày hiện tại so với Thời hạn hoàn thành. */
            String deadlineState,
            String deadlineStateLabel,
            LocalDate lastEditedDate,
            String lastEditedBy,
            String note
    ) {
    }

    public record TransferResult(int transferred, int skipped) {
    }
}
