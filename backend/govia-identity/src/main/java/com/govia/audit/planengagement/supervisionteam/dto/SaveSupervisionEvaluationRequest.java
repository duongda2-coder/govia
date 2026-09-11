package com.govia.audit.planengagement.supervisionteam.dto;

public record SaveSupervisionEvaluationRequest(
        boolean progress,
        boolean contentAssured,
        boolean qualityAssured,
        String note
) {
}
