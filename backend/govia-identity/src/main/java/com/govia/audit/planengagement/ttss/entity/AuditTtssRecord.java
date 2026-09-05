package com.govia.audit.planengagement.ttss.entity;

import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * "Quản lý tồn tại sai sót" (TTSS) - sheet "Quản lý công việc" trong Tạo CKT (1).xlsx, mục C.
 * HOAN TOAN TACH BIET voi {@code com.govia.audit.finding.entity.AuditFinding} (entity do la nguon
 * du lieu cho AI Agent tool get_audit_findings/get_evidence, xem docs/kien-truc-ky-thuat/
 * audit-tools-contract.md - khong duoc dung chung/mo rong entity do). Moi lan "Upload file TTSS"
 * tao MOI cac dong (khong upsert theo khoa tu nhien nhu CmNtd1..14 vi TTSS khong co khoa nghiep vu
 * on dinh) - giu lai lich su theo tung lan upload.
 */
@Getter
@Setter
@Entity
@Table(name = "audit_ttss_record")
public class AuditTtssRecord extends BaseEntity {

    @Column(name = "engagement_id", nullable = false, columnDefinition = "uuid")
    private UUID engagementId;

    /** "NV" - mang nghiep vu, tham chieu AuditMasterDataItem category BUSINESS_SEGMENT. */
    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;

    /** "User Name" - text tu do, khong FK (dung dac ta cot rieng biet voi assignedEmployeeId). */
    @Column(name = "record_username", length = 100)
    private String recordUsername;

    /** "Mã công việc" - text tu do, khop long voi AuditWorkItem.code (khong FK cung dac ta, giong
     * quy uoc detailCode - xem AuditWorkItem). */
    @Column(name = "work_item_code", length = 10)
    private String workItemCode;

    /** "Bước QT tổng hợp" - FK that toi danh muc da co san. */
    @Column(name = "process_step_summary_id", columnDefinition = "uuid")
    private UUID processStepSummaryId;

    /** "Mã BQT chi tiết" - FK that toi danh muc da co san. */
    @Column(name = "process_step_detail_id", columnDefinition = "uuid")
    private UUID processStepDetailId;

    /** "Nội dung TTSS" - dien giai chi tiet ve ton tai sai sot. */
    @Column(name = "ttss_content", length = 4000)
    private String ttssContent;

    @Column(name = "finding_code", length = 50)
    private String findingCode;

    @Column(name = "finding_name", length = 1000)
    private String findingName;

    /** "Trọng yếu". */
    @Column(name = "material", nullable = false)
    private boolean material = false;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "reference_number_2", length = 100)
    private String referenceNumber2;

    @Column(name = "customer_code", length = 50)
    private String customerCode;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "amount", precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "performing_user", length = 100)
    private String performingUser;

    @Column(name = "transaction_content", length = 2000)
    private String transactionContent;

    /** "Ngày HTKN" - ngay hach toan/khoi tao nghiep vu (thoi diem phat sinh giao dich duoc TTSS). */
    @Column(name = "exception_date")
    private LocalDate exceptionDate;

    @Column(name = "approver_name", length = 255)
    private String approverName;

    @Column(name = "controller_name", length = 255)
    private String controllerName;

    /** "cán bộ thực hiện TTSS" - tu dong = ten day du nguoi upload, khong cho user tu sua. */
    @Column(name = "ttss_performer_name", length = 255)
    private String ttssPerformerName;

    @Column(name = "related_staff", length = 500)
    private String relatedStaff;

    /** "Mã KN"/"Tên KN" - kien nghi nguoi UPLOAD tu ghi (chi la goi y, khac voi
     * teamRecommendationId ben duoi - kien nghi CHINH THUC do truong nhom/truong doan gan). */
    @Column(name = "uploader_recommendation_code", length = 30)
    private String uploaderRecommendationCode;

    @Column(name = "uploader_recommendation_name", length = 255)
    private String uploaderRecommendationName;

    /** "Mã KN trưởng đoàn" - kien nghi CHINH THUC, set qua chuc nang "Gắn kiến nghị". Noi dung
     * ("Nội dung KN trưởng đoàn") KHONG luu rieng - resolve dong tu day luc tra response de tranh
     * trung lap/lech du lieu voi bang danh muc AuditRecommendation. */
    @Column(name = "team_recommendation_id", columnDefinition = "uuid")
    private UUID teamRecommendationId;

    /** "Phê duyệt KN" - null = chua nop duyet (chua gan kien nghi hoac da gan nhung chua bam duyet). */
    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_approval_status", length = 20)
    private AssignmentApprovalStatus recommendationApprovalStatus;

    @Column(name = "recommendation_approved_by", length = 100)
    private String recommendationApprovedBy;

    @Column(name = "recommendation_approved_at")
    private Instant recommendationApprovedAt;

    @Column(name = "recommendation_process_instance_id", length = 64)
    private String recommendationProcessInstanceId;
}
