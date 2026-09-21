package com.govia.audit.tdkp.common;

import com.govia.core.web.BusinessException;

/** "Đối tượng được kiến nghị" (HĐTV / TGĐ). */
public enum TdkpTarget {
    HDTV("HĐTV"),
    TGD("TGĐ");

    private final String label;

    TdkpTarget(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static TdkpTarget parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        for (TdkpTarget target : values()) {
            if (target.name().equalsIgnoreCase(value) || target.label.equalsIgnoreCase(value)) {
                return target;
            }
        }
        throw new BusinessException("IMPORT_LIST_VALUE_NOT_FOUND", "Doi tuong duoc kien nghi khong hop le (HDTV/TGD): " + value);
    }

    public static String labelOf(TdkpTarget target) {
        return target == null ? null : target.label;
    }
}
