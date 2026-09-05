package com.govia.audit.planengagement.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** "Phan cong nghiep vu cho thanh vien" - 1 cong viec kiem toan (AuditWorkItem) duoc giao cho 1
 * thanh vien nhom. Duoc tao tu dong khi them thanh vien (theo nghiep vu 1/2/3 cua thanh vien do),
 * hoac them thu cong qua nut "Chon cong viec". Cac cot status/note/approval* phuc vu man hinh
 * "Quản lý công việc" (sheet "Tạo CKT (1).xlsx" - CBKT/THKT): user tu cap nhat trang thai, truong
 * doan phe duyet hang loat qua quy trinh Flowable "audit_workitem_approval" (xem
 * AuditWorkAssignmentService, AuditWorkApprovalChainResolver). */
@Getter
@Setter
@Entity
@Table(name = "audit_engagement_assignment")
public class AuditEngagementAssignment extends BaseEntity {

    @Column(name = "group_member_id", nullable = false, columnDefinition = "uuid")
    private UUID groupMemberId;

    @Column(name = "work_item_id", nullable = false, columnDefinition = "uuid")
    private UUID workItemId;

    /** "Trạng thái công việc" - user tu cap nhat trong man hinh Quan ly cong viec. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AssignmentStatus status = AssignmentStatus.NOT_STARTED;

    /** "Ghi chú" - user tu nhap. */
    @Column(name = "note", length = 2000)
    private String note;

    /** "Trưởng đoàn phê duyệt" - null = chua nop duyet lan nao. */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20)
    private AssignmentApprovalStatus approvalStatus;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    /** Id process instance Flowable ("audit_workitem_approval") gan nhat da chay cho dong nay -
     * dung de tra lich su/task qua GET /api/workflow/process-definitions... (khong bat buoc hien
     * thi trong man hinh Quan ly cong viec dot nay). */
    @Column(name = "process_instance_id", length = 64)
    private String processInstanceId;
}
