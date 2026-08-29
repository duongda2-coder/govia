package com.govia.audit.riskscoring.scoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Luu 1 dong wide-format cung luc: null trong values xoa gia tri chi tieu do (neu da co), gia tri
 * khac null tao/cap nhat. Key cua map la ma chi tieu dinh luong (RiskCriteriaQuantitative.code). */
public record RiskCriteriaQuantitativeWideRowRequest(
        @NotBlank String branchCode,
        @NotNull Integer year,
        LocalDate entryDate,
        Map<String, BigDecimal> valuesByCriteriaCode
) {
}
