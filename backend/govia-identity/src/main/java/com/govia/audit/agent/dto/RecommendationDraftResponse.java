package com.govia.audit.agent.dto;

import java.util.List;

/**
 * Ban nhap kien nghi do A4 soan - CHUA luu gi. Nguoi dung chon "Dùng nội dung này" de dua vao form
 * "Thêm kiến nghị" roi tu bam Them (qua API kien nghi san co). grounded=false o 1 ban nhap nghia la
 * ban nhap do nhac ma phat hien/so lieu khong co trong du lieu TTSS dua vao - giao dien canh bao.
 */
public record RecommendationDraftResponse(
        String engagementCode,
        String businessSegmentCode,
        int sourceTtssCount,
        List<Draft> drafts,
        List<SimilarRecommendation> similarRecommendations,
        String model
) {
    public record Draft(String content, List<String> findingCodes, String rationale, boolean grounded) {
    }

    public record SimilarRecommendation(String engagementCode, String code, String content) {
    }
}
