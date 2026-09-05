package com.govia.audit.planengagement.workflow;

import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.audit.planengagement.repository.AuditEngagementAssignmentRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Gan vao end event "endRejected" cua quy trinh audit_workitem_approval - danh cho tuong lai khi
 * co buoc duyet co the tu choi (hien tai AuditWorkAssignmentService luon complete voi approved=
 * true nen nhanh nay chua duoc kich hoat trong dot nay, nhung BPMN/listener san sang san).
 */
@Component
public class AuditWorkItemApprovalRejectedListener implements ExecutionListener {

    private final AuditEngagementAssignmentRepository assignmentRepository;
    private final AuditLogService auditLogService;

    public AuditWorkItemApprovalRejectedListener(AuditEngagementAssignmentRepository assignmentRepository, AuditLogService auditLogService) {
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

        assignmentRepository.findById(assignmentId).ifPresent(assignment -> {
            assignment.setApprovalStatus(AssignmentApprovalStatus.REJECTED);
            assignmentRepository.save(assignment);
            auditLogService.record("AuditEngagementAssignment", assignmentId, AuditAction.REJECT,
                    "Tu choi phe duyet cong viec qua quy trinh phe duyet (audit_workitem_approval)");
        });
    }
}
