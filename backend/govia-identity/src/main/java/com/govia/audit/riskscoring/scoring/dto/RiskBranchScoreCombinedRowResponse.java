package com.govia.audit.riskscoring.scoring.dto;

import java.math.BigDecimal;
import java.util.Map;

/** 1 dong = 1 chi nhanh/nam (sheet CT_Diem_All) - moi nghiep vu (ma nghiep vu, khop domain cua
 * RiskWeightByBusiness.businessCode) la 1 entry trong scoresByBusinessLineCode, da la diem quy doi
 * (gop dinh tinh + dinh luong theo ti trong nghiep vu, hoac lay thang neu nghiep vu do chi co 1
 * ben). Xem RiskBranchScoreCombinedService. */
public record RiskBranchScoreCombinedRowResponse(
        String branchCode,
        String branchName,
        Integer year,
        BigDecimal totalScore,
        String rankLabel,
        Map<String, BigDecimal> scoresByBusinessLineCode
) {
}
