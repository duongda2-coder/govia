package com.govia.audit.masterdata.entity;

/** Nhom hien thi danh muc tren UI (gop nhieu category vao 1 man hinh dang tab) - khong luu DB. */
public enum MasterDataGroup {
    AUDIT("Kiểm toán"),
    FINDING("Phát hiện & Khắc phục"),
    RISK("Rủi ro"),
    CONTROL("Kiểm soát"),
    PROCESS("Quy trình"),
    COMPLIANCE("Tuân thủ"),
    GENERAL("Chung");

    private final String label;

    MasterDataGroup(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
