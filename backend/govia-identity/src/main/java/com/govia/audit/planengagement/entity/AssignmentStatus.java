package com.govia.audit.planengagement.entity;

/** "Trạng thái công việc" cua 1 AuditEngagementAssignment - user tu cap nhat trong man hinh
 * "Quản lý công việc" (CBKT/THKT). CHUA hoan thanh thi khong duoc dua vao dot phe duyet hang loat
 * (xem AuditWorkAssignmentService.approve). */
public enum AssignmentStatus {
    NOT_STARTED, IN_PROGRESS, DONE
}
