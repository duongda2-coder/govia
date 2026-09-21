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

import java.time.LocalDate;
import java.util.UUID;

/** "Bảng theo dõi kiến nghị tổng hợp" (sheet 4 ZTC_TDKP_CN): 1 dòng = 1 kiến nghị của KTNB đối với 1 chi nhánh trong 1 cuộc kiểm toán.
 * Dòng được chuyển từ phân hệ Thực hiện kiểm toán (kiến nghị trưởng đoàn đã phê duyệt ở TTSS) nên nhớ engagementId +
 * sourceRecommendationId để không chuyển trùng; cũng cho nhập tay/import (khi đó không có nguồn). */
@Getter
@Setter
@Entity
@Table(name = "audit_tdkp_branch_recommendation")
public class AuditTdkpBranchRecommendation extends BaseEntity {

    /** "Mã quản lý kiến nghị" - hệ thống tự sinh (KN001, KN002...). */
    @Column(name = "management_code", nullable = false, length = 30)
    private String managementCode;

    /** "Chi nhánh" / "Mã chi nhánh" - đối tượng kiểm toán (AuditObjectUnit); tên và mã suy ra từ đơn vị này. */
    @Column(name = "audit_object_unit_id", nullable = false, columnDefinition = "uuid")
    private UUID auditObjectUnitId;

    @Column(name = "engagement_id", columnDefinition = "uuid")
    private UUID engagementId;

    @Column(name = "source_recommendation_id", columnDefinition = "uuid")
    private UUID sourceRecommendationId;

    /** "Năm KT" - lấy từ cuộc kiểm toán (ztc_job) khi chuyển. */
    @Column(name = "audit_year")
    private Integer auditYear;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Column(name = "recommendation_type_id", columnDefinition = "uuid")
    private UUID recommendationTypeId;

    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;

    @Column(name = "deadline")
    private LocalDate deadline;

    /** "Nội dung chỉnh sửa". */
    @Column(name = "edit_content", length = 500)
    private String editContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private TdkpStatus status;

    @Column(name = "evaluation", length = 500)
    private String evaluation;

    @Column(name = "note", length = 500)
    private String note;
}
