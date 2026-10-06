package com.govia.audit.tools.dto;

import java.math.BigDecimal;

/**
 * Bien dong diem rui ro tong hop cua 1 chi nhanh giua 2 nam - tinh tu 2 lan doc ket qua cham diem tong
 * hop co san (khong tinh lai diem). scoreChange = totalScore - previousTotalScore.
 */
public record ScoreChangeResponse(
        String branchCode,
        String branchName,
        Integer year,
        BigDecimal totalScore,
        String rankLabel,
        Integer previousYear,
        BigDecimal previousTotalScore,
        String previousRankLabel,
        BigDecimal scoreChange,
        boolean rankChanged
) {
}
