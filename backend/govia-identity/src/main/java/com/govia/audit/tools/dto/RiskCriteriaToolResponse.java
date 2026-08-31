package com.govia.audit.tools.dto;

import java.math.BigDecimal;

/**
 * 1 dinh nghia tieu chi cham diem, chuan hoa chung cho ca 3 loai (kind = "quantitative" |
 * "qualitative" | "other") de tool "get_risk_criteria" tra ve 1 hinh dang duy nhat cho AI Agent.
 * Field khong ap dung cho loai do se la null (vd criteriaType/score20.. chi co o quantitative,
 * impactLevel/likelihoodLevel chi co o qualitative) - KHONG suy dien gia tri thay the.
 */
public record RiskCriteriaToolResponse(
        String kind,
        String code,
        String name,
        BigDecimal weight,
        String group1Code,
        String group2Code,
        String groupHoCode,
        String riskTypeHoCode,
        Integer criteriaType,
        BigDecimal businessThreshold,
        BigDecimal viewThreshold,
        BigDecimal score20,
        BigDecimal score40,
        BigDecimal score60,
        BigDecimal score80,
        BigDecimal score100,
        String scoringGuide,
        Integer impactLevel,
        Integer likelihoodLevel,
        boolean active
) {
}
