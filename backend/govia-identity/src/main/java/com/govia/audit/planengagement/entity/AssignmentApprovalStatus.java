package com.govia.audit.planengagement.entity;

/** "Trưởng đoàn phê duyệt" cua 1 AuditEngagementAssignment - null = chua tung nop duyet.
 * PENDING chi ton tai trong khoang ngan giua luc start quy trinh Flowable va luc task duyet hoan
 * tat (xem AuditWorkItemApprovalApprovedListener/RejectedListener). */
public enum AssignmentApprovalStatus {
    PENDING, APPROVED, REJECTED
}
