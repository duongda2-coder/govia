package com.govia.audit.riskscoring.scoring.dto;

import java.util.UUID;

/**
 * 1 dong "phang" = 1 header + 1 line (dung dinh dang voi file Excel export/import cua man hinh
 * "Cham diem rui ro khac" - moi dong ung voi 1 chi tieu duoc cham cua 1 ky Loai doi tuong KT + Ma
 * doi tuong KT + Nam). Dung cho man hinh danh sach hien 1 dong/1 chi tieu thay vi 1 dong/1 header.
 */
public record RiskAssessmentOtherRowResponse(
        UUID headerId,
        UUID auditObjectCategoryId,
        String auditObjectCategoryCode,
        String auditObjectCategoryName,
        String auditObjectCode,
        String auditObjectName,
        Integer year,
        boolean active,
        UUID lineId,
        UUID criteriaOtherId,
        String criteriaOtherCode,
        String criteriaOtherName,
        UUID scaleId,
        Integer scaleScore,
        String ratingLevel
) {
}
