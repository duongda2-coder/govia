package com.govia.audit.masterdata.entity;

/** Nhom hien thi danh muc tren UI (gop nhieu category vao 1 man hinh dang tab) - khong luu DB. */
public enum MasterDataGroup {
    RISK("Rủi ro"),
    GENERAL("Chung"),
    POSITION("Chức vụ"),
    DEPARTMENT("Bộ phận KT"),
    YEAR("Năm"),
    BUSINESS_SEGMENT("Mảng nghiệp vụ");

    private final String label;

    MasterDataGroup(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
