package com.govia.audit.riskscoring.scoring.dto;

import java.math.BigDecimal;
import java.util.Map;

/** 1 dong = 1 chi nhanh/nam (sheet CT_Diem_DT) - moi nhom cap 2 (RiskGroup2) la 1 entry trong
 * scoresByGroup2Code, da la tong diem dong gop cua tat ca chi tieu thuoc nhom do (khong phai gia
 * tri HSRR tho), giong tinh than voi RiskBranchScoreQuantitativeRowResponse nhung gom o muc group2
 * thay vi tung chi tieu rieng (theo dung layout sheet CT_Diem_DT). */
public record RiskBranchScoreQualitativeRowResponse(
        String branchCode,
        String branchName,
        Integer year,
        BigDecimal totalScore,
        String rankLabel,
        Map<String, BigDecimal> scoresByGroup2Code
) {
}
