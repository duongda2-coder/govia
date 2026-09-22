package com.govia.audit.phbc.common;

import com.govia.core.web.BusinessException;

/** "Mã kiến nghị" cố định (M01-M04) - NSD chọn 1 trong 4, Tên KN suy ra trực tiếp từ mã, không phải danh mục quản trị được. */
public enum ReportRecommendationCode {
    M01("Nhóm cơ chế, chính sách"),
    M02("Nhóm kiến nghị về giám sát an toàn hoạt động"),
    M03("Nhóm kiến nghị về hướng dẫn thực hiện cơ chế, quy chế"),
    M04("Các chỉ đạo khác");

    private final String label;

    ReportRecommendationCode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static ReportRecommendationCode parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        for (ReportRecommendationCode code : values()) {
            if (code.name().equalsIgnoreCase(value) || code.label.equalsIgnoreCase(value)) {
                return code;
            }
        }
        throw new BusinessException("IMPORT_LIST_VALUE_NOT_FOUND", "Ma kien nghi khong hop le (M01-M04): " + value);
    }

    public static String labelOf(ReportRecommendationCode code) {
        return code == null ? null : code.label;
    }
}
