package com.govia.audit.planengagement.progressreport.entity;

import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * "Báo cáo tiến độ" (Khối B) - 1 chức năng TRONG màn hình "Quản lý công việc THKT" đã có (không
 * phải màn hình/permission riêng, xem AUDIT.WORK_MANAGEMENT.*). Được sinh TỰ ĐỘNG mỗi lần "Upload
 * file TTSS" (xem AuditTtssService/AuditProgressReportService.recordUpload) - không có API tạo
 * thủ công. File báo cáo đính kèm dùng bảng {@code attachment} dùng chung
 * (entityName="AUDIT_PROGRESS_REPORT", entityId=report.id), không thêm cột riêng.
 */
@Getter
@Setter
@Entity
@Table(name = "audit_progress_report")
public class AuditProgressReport extends BaseEntity {

    @Column(name = "engagement_id", nullable = false, columnDefinition = "uuid")
    private UUID engagementId;

    /** "mã mảng nv" - tham chieu AuditMasterDataItem category BUSINESS_SEGMENT. */
    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;

    @Column(name = "reported_employee_id", columnDefinition = "uuid")
    private UUID reportedEmployeeId;

    @Column(name = "total_findings", nullable = false)
    private int totalFindings;

    @Column(name = "total_ttss", nullable = false)
    private int totalTtss;

    @Column(name = "total_material_findings", nullable = false)
    private int totalMaterialFindings;

    @Column(name = "total_material_ttss", nullable = false)
    private int totalMaterialTtss;

    /** Tổng số lượng mẫu - proxy = đếm AuditEngagementAssignment theo user+mảng NV (không đếm mẫu
     * thật ở 16 bảng chọn mẫu CmNtd/CmTd - xem ghi chú trong plan). */
    @Column(name = "total_samples", nullable = false)
    private int totalSamples;

    @Column(name = "completed_samples", nullable = false)
    private int completedSamples;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    /** "Lần báo cáo" - tăng dần theo (engagementId, businessSegmentId, reportedEmployeeId). */
    @Column(name = "report_round", nullable = false)
    private int reportRound;

    @Column(name = "reported_by_username", length = 100)
    private String reportedByUsername;

    @Column(name = "note", length = 2000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20)
    private AssignmentApprovalStatus approvalStatus;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "process_instance_id", length = 64)
    private String processInstanceId;
}
