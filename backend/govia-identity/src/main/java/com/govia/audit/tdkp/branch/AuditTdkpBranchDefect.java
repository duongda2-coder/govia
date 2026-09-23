package com.govia.audit.tdkp.branch;

import com.govia.audit.tdkp.common.TdkpStatus;
import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** "Bảng theo dõi chi tiết tình hình chỉnh sửa tồn tại sai sót" (sheet 4 ZTC_TDKP_CN): mỗi dòng = 1 tồn tại sai sót (TTSS) liên quan tới
 * 1 kiến nghị ở bảng tổng hợp. */
@Getter
@Setter
@Entity
@Table(name = "audit_tdkp_branch_defect")
public class AuditTdkpBranchDefect extends BaseEntity {

    @Column(name = "branch_recommendation_id", nullable = false, columnDefinition = "uuid")
    private UUID branchRecommendationId;

    /** TTSS nguồn (audit_ttss_record) - chống chuyển trùng. */
    @Column(name = "source_ttss_id", columnDefinition = "uuid")
    private UUID sourceTtssId;

    /** "Sai sót liên quan đến kiến nghị". */
    @Column(name = "defect_content", length = 2000)
    private String defectContent;

    /** "Khách hàng/Bút toán" - "Sai sót liên quan đến khách hàng". */
    @Column(name = "customer_entry", length = 500)
    private String customerEntry;

    /** "Hợp đồng tín dụng liên quan (LAV)". */
    @Column(name = "credit_contract", length = 100)
    private String creditContract;

    /** "Mã tồn tại sai sót" - khop voi AuditTtssRecord.findingCode khi chuyen tu Thuc hien kiem toan. */
    @Column(name = "defect_code", length = 50)
    private String defectCode;

    /** "Loại sai sót". */
    @Column(name = "defect_type", length = 255)
    private String defectType;

    /** "Hiện trạng chỉnh sửa sai sót liên quan đến khách hàng" - NSD chọn Đã/Đang/Chưa thực hiện. */
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_status", length = 20)
    private TdkpStatus customerStatus;

    /** "Cán bộ liên quan" - CBNV tại chi nhánh đi kiểm toán. */
    @Column(name = "related_staff", length = 255)
    private String relatedStaff;
}
