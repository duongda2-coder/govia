package com.govia.audit.planengagement.ttss.dto;

import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AuditTtssRecordResponse(
        UUID id,
        UUID engagementId,
        UUID businessSegmentId,
        String businessSegmentCode,
        String recordUsername,
        String workItemCode,
        UUID processStepSummaryId,
        String processStepSummaryCode,
        String processStepSummaryName,
        UUID processStepDetailId,
        String processStepDetailCode,
        String ttssContent,
        String findingCode,
        String findingName,
        boolean material,
        String referenceNumber,
        String referenceNumber2,
        String customerCode,
        String customerName,
        BigDecimal amount,
        String performingUser,
        String transactionContent,
        LocalDate exceptionDate,
        String approverName,
        String controllerName,
        String ttssPerformerName,
        String relatedStaff,
        String uploaderRecommendationCode,
        String uploaderRecommendationName,
        String appendix,
        List<TeamRecommendation> teamRecommendations,
        AssignmentApprovalStatus recommendationApprovalStatus,
        String recommendationApprovedBy,
        Instant recommendationApprovedAt
) {

    /** 1 kien nghi truong doan da gan cho dong TTSS (1 TTSS co the gan nhieu kien nghi - test25.9). */
    public record TeamRecommendation(UUID id, String code, String content) {
    }
}
