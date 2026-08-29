package com.govia.audit.riskscoring.scoring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** 1 dong = 1 chi nhanh/nam (nhu sheet DL_Nhaptructiep / mau DL_HSRR_Upload) - moi chi tieu dinh
 * luong la 1 entry trong valuesByCriteriaCode thay vi 1 dong rieng nhu RiskCriteriaQuantitativeValueResponse. */
public record RiskCriteriaQuantitativeWideRowResponse(
        String branchCode,
        String branchName,
        Integer year,
        LocalDate entryDate,
        Map<String, BigDecimal> valuesByCriteriaCode
) {
}
