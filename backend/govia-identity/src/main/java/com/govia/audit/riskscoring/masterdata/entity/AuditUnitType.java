package com.govia.audit.riskscoring.masterdata.entity;

/**
 * Loai don vi cua doi tuong kiem toan HO/Giam sat CC/Chi nhanh (sheet ZTC_DTKT1) - lay dung 3 gia
 * tri trong ten man hinh goc ("Danh muc doi tuong kiem toan HO, Giam sat CC, Chi nhanh"), vi tai
 * lieu khong cap catalog rieng "ZTC_loai_DTKT" ma cot "Loai don vi" tham chieu toi.
 */
public enum AuditUnitType {
    HO("Hội sở"),
    GSCC("Giám sát chi nhánh"),
    CN("Chi nhánh");

    private final String label;

    AuditUnitType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
