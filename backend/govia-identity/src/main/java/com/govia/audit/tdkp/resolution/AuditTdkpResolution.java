package com.govia.audit.tdkp.resolution;

import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
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

/** Theo dõi tình hình thực hiện nghị quyết của HĐTV (sheet 5 ZTC_TDKP_NQ). Cac cot "Đánh giá
 * (Đã/Đang/Chưa thực hiện)"/"Người giám sát"/"Đánh giá về việc ban hành NQ của HĐTV" (500 ky tu) ban
 * dau da bi bo/rut gon theo phan hoi nguoi dung (test23.9) - xem progressStatus/issuanceEvaluation
 * (100 ky tu) thay the. */
@Getter
@Setter
@Entity
@Table(name = "audit_tdkp_resolution")
public class AuditTdkpResolution extends BaseEntity {

    /** "Mã quản lý kiến nghị nghị quyết" - hệ thống tự sinh (NQ001...). */
    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "resolution_number", nullable = false, length = 20)
    private String resolutionNumber;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    /** "Trích yếu nghị quyết". */
    @Column(name = "summary", length = 255)
    private String summary;

    @Column(name = "content", length = 500)
    private String content;

    /** "Công việc chi tiết". */
    @Column(name = "work_detail", length = 255)
    private String workDetail;

    /** "Lĩnh vực". */
    @Column(name = "field_area", length = 100)
    private String fieldArea;

    /** "Đơn vị" - chon trong danh muc doi tuong kiem toan co loai doi tuong = HO (Hoi so). */
    @Column(name = "unit_id", columnDefinition = "uuid")
    private UUID unitId;

    /** "Người liên hệ". */
    @Column(name = "contact_person", length = 255)
    private String contactPerson;

    /** "NQ liên quan có cùng ND" - nhap tay, khong FK. */
    @Column(name = "related_resolution", length = 100)
    private String relatedResolution;

    /** "Thời hạn hoàn thành". */
    @Column(name = "completion_deadline")
    private LocalDate completionDeadline;

    /** "Căn cứ thời hạn hoàn thành". */
    @Column(name = "completion_deadline_basis", length = 100)
    private String completionDeadlineBasis;

    /** "Tình hình thực hiện". */
    @Column(name = "implementation", length = 500)
    private String implementation;

    /** "Tình trạng" (ĐANG THỰC HIỆN/CHƯA THỰC HIỆN/HOÀN THÀNH) - dung chung enum TdkpStatus, hien thi
     * nhan rieng cho man hinh nay (xem tdkpShared/i18n auditTdkp.resolution.progressStatus). */
    @Enumerated(EnumType.STRING)
    @Column(name = "progress_status", length = 20)
    private TdkpStatus progressStatus;

    /** "Lý do". */
    @Column(name = "reason", length = 100)
    private String reason;

    /** "Đánh giá việc ban hành NQ". */
    @Column(name = "issuance_evaluation", length = 100)
    private String issuanceEvaluation;

    /** "Thời gian hoàn thành" - ngay hoan thanh thuc te (khac completionDeadline = thoi han). */
    @Column(name = "completion_date")
    private LocalDate completionDate;

    /** "Nhóm theo dõi" (QTDH/TD/NTD) - 3 gia tri co dinh, cung quy uoc voi module KHNS_NAM. */
    @Column(name = "follow_up_group", length = 10)
    private String followUpGroup;

    /** "Người theo dõi". */
    @Column(name = "follower_name", length = 100)
    private String followerName;

    /** "Đề xuất". */
    @Column(name = "proposal", length = 100)
    private String proposal;

    /** "Lý do đề xuất". */
    @Column(name = "proposal_reason", length = 100)
    private String proposalReason;

    @Column(name = "note", length = 500)
    private String note;

    /** "Trạng thái phê duyệt" - nhap/chon tay, khong gan voi quy trinh Flowable. */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20)
    private AssignmentApprovalStatus approvalStatus;
}
