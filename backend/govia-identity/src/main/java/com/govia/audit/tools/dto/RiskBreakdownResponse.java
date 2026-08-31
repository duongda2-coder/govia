package com.govia.audit.tools.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Chi tiet diem rui ro cua 1 chi nhanh/nam, ghep tu 3 nguon that: tong diem quy doi theo tung
 * nghiep vu (RiskBranchScoreCombinedService), diem theo tung chi tieu dinh luong
 * (RiskBranchScoreQuantitativeService.scoresByCriteriaCode) va theo tung nhom cap 2 dinh tinh
 * (RiskBranchScoreQualitativeService.scoresByGroup2Code). scoresByCriteriaQuantitative/
 * scoresByGroup2Qualitative la null neu chi nhanh khong co du lieu ben do trong nam - KHONG suy dien
 * thanh 0.
 */
public record RiskBreakdownResponse(
        String branchCode,
        String branchName,
        Integer year,
        BigDecimal totalScore,
        String rankLabel,
        Map<String, BigDecimal> scoresByBusinessLine,
        Map<String, BigDecimal> scoresByCriteriaQuantitative,
        Map<String, BigDecimal> scoresByGroup2Qualitative
) {
}
