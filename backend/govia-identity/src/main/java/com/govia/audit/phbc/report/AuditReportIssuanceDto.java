package com.govia.audit.phbc.report;

import com.govia.audit.phbc.common.ReportIssuingUnit;
import com.govia.audit.phbc.common.ReportRecommendationCode;
import com.govia.audit.phbc.common.ReportRecommendationTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public final class AuditReportIssuanceDto {

    private AuditReportIssuanceDto() {
    }

    public record ReportRequest(
            @NotBlank @Size(max = 100) String reportNumber,
            LocalDate reportDate,
            @NotBlank @Size(max = 255) String reportName,
            @Size(max = 1000) String summaryContent,
            ReportIssuingUnit issuingUnit
    ) {
    }

    public record ReportResponse(
            UUID id,
            String reportNumber,
            LocalDate reportDate,
            String reportName,
            String summaryContent,
            ReportIssuingUnit issuingUnit,
            String issuingUnitLabel,
            /** Số kiến nghị đã thêm vào báo cáo (bảng con). */
            int recommendationCount
    ) {
    }

    public record RecommendationRequest(
            @NotNull UUID reportIssuanceId,
            @NotNull ReportRecommendationCode recommendationCode,
            @NotBlank @Size(max = 1000) String content,
            UUID businessSegmentId,
            ReportRecommendationTarget target,
            UUID executingUnitId,
            @Size(max = 100) String executingUnitName,
            LocalDate deadline
    ) {
    }

    public record RecommendationResponse(
            UUID id,
            UUID reportIssuanceId,
            String reportNumber,
            ReportRecommendationCode recommendationCode,
            String recommendationName,
            String content,
            UUID businessSegmentId,
            String businessSegmentCode,
            String businessSegmentName,
            ReportRecommendationTarget target,
            String targetLabel,
            UUID executingUnitId,
            String executingUnitCode,
            String executingUnitName,
            LocalDate deadline
    ) {
    }
}
