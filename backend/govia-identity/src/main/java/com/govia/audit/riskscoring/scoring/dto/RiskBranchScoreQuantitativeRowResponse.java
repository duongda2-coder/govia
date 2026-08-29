package com.govia.audit.riskscoring.scoring.dto;

import java.math.BigDecimal;
import java.util.Map;

/** 1 dong = 1 chi nhanh/nam (sheet CT_Diem_DL) - moi chi tieu dinh luong la 1 entry trong
 * scoresByCriteriaCode (da la diem dong gop, khong phai gia tri HSRR tho) de FE tu dung cot dong
 * theo nhom (group1) + ma chi tieu, giong pattern da dung cho RiskCriteriaQuantitativeWideRowResponse. */
public record RiskBranchScoreQuantitativeRowResponse(
        String branchCode,
        String branchName,
        Integer year,
        BigDecimal totalScore,
        String rankLabel,
        Map<String, BigDecimal> scoresByCriteriaCode
) {
}
