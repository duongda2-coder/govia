package com.govia.audit.phbc.common;

import com.govia.core.web.BusinessException;

/** "Đối tượng kiến nghị": chọn 1 trong 3 HĐTV, TGĐ, ALL (khác TdkpTarget của module TDKP vì có thêm ALL). */
public enum ReportRecommendationTarget {
    HDTV("HĐTV"),
    TGD("TGĐ"),
    ALL("ALL");

    private final String label;

    ReportRecommendationTarget(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static ReportRecommendationTarget parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        for (ReportRecommendationTarget target : values()) {
            if (target.name().equalsIgnoreCase(value) || target.label.equalsIgnoreCase(value)) {
                return target;
            }
        }
        throw new BusinessException("IMPORT_LIST_VALUE_NOT_FOUND", "Doi tuong kien nghi khong hop le (HDTV/TGD/ALL): " + value);
    }

    public static String labelOf(ReportRecommendationTarget target) {
        return target == null ? null : target.label;
    }
}
