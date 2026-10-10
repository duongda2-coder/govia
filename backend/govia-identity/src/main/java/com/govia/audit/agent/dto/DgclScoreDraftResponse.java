package com.govia.audit.agent.dto;

import java.util.List;

/**
 * Goi y cham DGCL - CHI LA GOI Y de nguoi cham tham khao; khong luu vao phieu. Diem/tich van do nguoi cham tu nhap,
 * Lưu, Xác nhận tren man hinh Danh gia chat luong.
 * suggestion: PL01A/PL01B = COMPLIANT | NON_COMPLIANT | NEED_REVIEW; PL01F = APPLIES | NOT_APPLIES | NEED_REVIEW.
 * current: gia tri dang co tren phieu (NOT_SET = chua cham). differsFromCurrent = goi y khac gia tri nguoi cham da nhap.
 */
public record DgclScoreDraftResponse(
        String engagementCode,
        String subject,
        String appendix,
        boolean sheetSaved,
        boolean sheetConfirmed,
        Double currentScore,
        int criteriaCount,
        boolean truncated,
        List<Item> items,
        int suggestedPositive,
        int suggestedNegative,
        int needReview,
        /** So goi y cua AI bi he thong ha ve "can xem ho so" vi khong chi ra duoc can cu co that trong ho so. */
        int downgraded,
        /** PL01A/PL01B: ty le tuan thu neu ap dung goi y (bo dong CAN_XEM); PL01F: null. */
        Double suggestedRatio,
        List<String> dossierFacts,
        String overall,
        boolean grounded,
        String model
) {

    public record Item(String key, String stt, String content, String current, String suggestion, Integer violationCount,
                       String reason, String evidence, boolean differsFromCurrent) {
    }
}
