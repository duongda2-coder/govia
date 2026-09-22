package com.govia.audit.phbc.common;

import com.govia.core.web.BusinessException;

/** "Đơn vị phát hành" báo cáo: NSD chọn KTNB hoặc BKS. */
public enum ReportIssuingUnit {
    KTNB("Kiểm toán nội bộ"),
    BKS("Ban kiểm soát");

    private final String label;

    ReportIssuingUnit(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static ReportIssuingUnit parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        for (ReportIssuingUnit unit : values()) {
            if (unit.name().equalsIgnoreCase(value) || unit.label.equalsIgnoreCase(value)) {
                return unit;
            }
        }
        throw new BusinessException("IMPORT_LIST_VALUE_NOT_FOUND", "Don vi phat hanh khong hop le (KTNB/BKS): " + value);
    }

    public static String labelOf(ReportIssuingUnit unit) {
        return unit == null ? null : unit.label;
    }
}
