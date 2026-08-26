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
    // Kiem toan
    AUDIT_TYPE("Loại hình kiểm toán", MasterDataGroup.AUDIT),
    AUDIT_CATEGORY("Phân loại cuộc kiểm toán", MasterDataGroup.AUDIT),
    AUDIT_METHODOLOGY("Phương pháp kiểm toán", MasterDataGroup.AUDIT),

    // Phat hien & khac phuc
    AUDIT_RATING("Xếp hạng phát hiện", MasterDataGroup.FINDING),
    AUDIT_OPINION("Kết luận chung cuộc kiểm toán", MasterDataGroup.FINDING),
    FINDING_CATEGORY("Phân loại phát hiện", MasterDataGroup.FINDING),
    RECOMMENDATION_TYPE("Loại kiến nghị", MasterDataGroup.FINDING),
    ACTION_TYPE("Loại hành động khắc phục", MasterDataGroup.FINDING),
    PRIORITY("Mức độ ưu tiên", MasterDataGroup.FINDING),

    // Rui ro
    RISK_CATEGORY("Nhóm rủi ro", MasterDataGroup.RISK),
    RISK_TYPE("Loại rủi ro", MasterDataGroup.RISK),
    RISK_LEVEL("Mức độ rủi ro", MasterDataGroup.RISK),

    // Kiem soat
    CONTROL_CATEGORY("Nhóm kiểm soát", MasterDataGroup.CONTROL),
    CONTROL_TYPE("Loại kiểm soát", MasterDataGroup.CONTROL),
    CONTROL_FREQUENCY("Tần suất kiểm soát", MasterDataGroup.CONTROL),
    CONTROL_EFFECTIVENESS_RATING("Đánh giá hiệu quả kiểm soát", MasterDataGroup.CONTROL),

    // Quy trinh
    BUSINESS_PROCESS("Quy trình nghiệp vụ", MasterDataGroup.PROCESS),
    BUSINESS_PROCESS_STEP("Bước quy trình", MasterDataGroup.PROCESS),

    // Tuan thu
    REGULATION("Luật/quy định", MasterDataGroup.COMPLIANCE),
    POLICY("Chính sách nội bộ", MasterDataGroup.COMPLIANCE),
    STANDARD("Chuẩn áp dụng", MasterDataGroup.COMPLIANCE),

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
    BUSINESS_SEGMENT("Mảng nghiệp vụ", MasterDataGroup.BUSINESS_SEGMENT);

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
