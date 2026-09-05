package com.govia.audit.planengagement.workflow;

import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.audit.planengagement.entity.AuditEngagementAssignment;
import com.govia.audit.planengagement.repository.AuditEngagementAssignmentRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Gan vao end event "endApproved" cua quy trinh audit_workitem_approval (xem
 * audit-workitem-approval.bpmn20.xml): khi day duyet dat, chuyen AuditEngagementAssignment sang
 * APPROVED.
 */
@Component
public class AuditWorkItemApprovalApprovedListener implements ExecutionListener {

    private final AuditEngagementAssignmentRepository assignmentRepository;
    private final AuditLogService auditLogService;

    public AuditWorkItemApprovalApprovedListener(AuditEngagementAssignmentRepository assignmentRepository, AuditLogService auditLogService) {
        this.assignmentRepository = assignmentRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public void notify(DelegateExecution execution) {
        Object assignmentIdValue = execution.getVariable("assignmentId");
        if (assignmentIdValue == null) {
            return;
        }
        UUID assignmentId = UUID.fromString(assignmentIdValue.toString());
        Object approverUsername = execution.getVariable("approverUsername");

        assignmentRepository.findById(assignmentId).ifPresent(assignment -> {
            assignment.setApprovalStatus(AssignmentApprovalStatus.APPROVED);
            assignment.setApprovedBy(approverUsername == null ? null : approverUsername.toString());
            assignment.setApprovedAt(Instant.now());
            assignmentRepository.save(assignment);
            auditLogService.record("AuditEngagementAssignment", assignmentId, AuditAction.APPROVE,
                    "Truong doan phe duyet cong viec qua quy trinh phe duyet (audit_workitem_approval)");
        });
    }
}
