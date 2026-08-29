package com.govia.audit.masterdata.entity;

/**
 * Toan bo danh muc cau hinh cua module Kiem toan noi bo - moi gia tri la 1 "loai danh muc" dung
 * chung 1 bang AuditMasterDataItem (phan biet qua cot category). Them/bot danh muc sau nay CHI can
 * them/xoa 1 dong enum o day, KHONG can migration DB moi hay man hinh moi (UI doc danh sach nay qua
 * GET /api/audit/master-data/categories va tu ve theo nhom).
 *
 * Co tinh CHUA co trong danh sach nay (xay dung rieng, khong phai danh muc don gian):
 * - audit_universe (chi la view tong hop Audit Entity, khong phai bang rieng)
 * - audit_entity (doi tuong kiem toan - doi tuong nghiep vu chinh, nhieu truong rieng)
 * - risk_matrix (bang quy doi 2 chieu, khac hinh dang danh muc phang)
 * - risk_kri (gan voi 1 Risk cu the + chuoi gia tri theo thoi gian, khong phai danh muc tinh)
 */
public enum AuditMasterDataCategory {
    // Rui ro
    RISK_CATEGORY("Nhóm rủi ro", MasterDataGroup.RISK),
    RISK_TYPE("Loại rủi ro", MasterDataGroup.RISK),
    RISK_LEVEL("Mức độ rủi ro", MasterDataGroup.RISK),

    // Chung
    CURRENCY("Tiền tệ", MasterDataGroup.GENERAL),
    COUNTRY("Quốc gia/Địa điểm", MasterDataGroup.GENERAL),
    FISCAL_PERIOD("Kỳ tài chính", MasterDataGroup.GENERAL),

    // Chuc vu (sheet ZTC_Chucvu)
    POSITION("Chức vụ", MasterDataGroup.POSITION),

    // Bo phan KT (sheet ZTC_BP)
    DEPARTMENT("Bộ phận KT", MasterDataGroup.DEPARTMENT),

    // Nam (sheet ZTC_Year)
    YEAR("Năm", MasterDataGroup.YEAR),

    // Mang nghiep vu (sheet ZTC_MANG_NV)
    BUSINESS_SEGMENT("Mảng nghiệp vụ", MasterDataGroup.BUSINESS_SEGMENT),

    // Loai don vi (tcode ZTC_LOAI_DTKT_CN)
    UNIT_TYPE("Loại đơn vị", MasterDataGroup.UNIT_TYPE);

    private final String label;
    private final MasterDataGroup group;

    AuditMasterDataCategory(String label, MasterDataGroup group) {
        this.label = label;
        this.group = group;
    }

    public String getLabel() {
        return label;
    }

    public MasterDataGroup getGroup() {
        return group;
    }
}
