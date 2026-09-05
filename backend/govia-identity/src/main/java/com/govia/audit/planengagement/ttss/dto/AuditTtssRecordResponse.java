package com.govia.audit.planengagement.ttss.dto;

import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
        UUID teamRecommendationId,
        String teamRecommendationCode,
        String teamRecommendationContent,
        AssignmentApprovalStatus recommendationApprovalStatus,
        String recommendationApprovedBy,
        Instant recommendationApprovedAt
) {
}
