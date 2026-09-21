package com.govia.audit.tdkp.common;

import com.govia.core.web.BusinessException;

/** "Hiện trạng" của kiến nghị/nghị quyết: Đã / Đang / Chưa thực hiện (list dùng chung mọi màn hình TDKP). */
public enum TdkpStatus {
    DONE("Đã thực hiện"),
    IN_PROGRESS("Đang thực hiện"),
    NOT_STARTED("Chưa thực hiện");

    private final String label;

    TdkpStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Nhận tên enum hoặc nhãn tiếng Việt (file Excel import); rỗng -> null, khác list -> lỗi dòng import. */
    public static TdkpStatus parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        for (TdkpStatus status : values()) {
            if (status.name().equalsIgnoreCase(value) || status.label.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new BusinessException("IMPORT_LIST_VALUE_NOT_FOUND", "Hien trang khong hop le (Da/Dang/Chua thuc hien): " + value);
    }

    public static String labelOf(TdkpStatus status) {
        return status == null ? null : status.label;
    }
}
