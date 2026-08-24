package com.govia.audit.riskscoring.masterdata.entity;

/**
 * Loai doi tuong ap dung chi tieu cham diem rui ro - CNDT/CNDL la chi nhanh (Dinh tinh/Dinh
 * luong), con lai la "Khac" theo tai lieu goc (HO, CNTT, DA). Dung chung cho Group1/Group2/
 * CriteriaQualitative/CriteriaQuantitative thay vi tao danh muc rieng (tai lieu khong cap sheet).
 */
public enum ObjectType {
    CNDT("Chi nhánh - Định tính"),
    CNDL("Chi nhánh - Định lượng"),
    HO("Hội sở"),
    CNTT("Công nghệ thông tin"),
    DA("Dự án");

    private final String label;

    ObjectType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
