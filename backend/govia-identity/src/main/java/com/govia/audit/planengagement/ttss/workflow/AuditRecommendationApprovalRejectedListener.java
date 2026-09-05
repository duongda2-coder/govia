package com.govia.audit.planengagement.ttss.workflow;

import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.audit.planengagement.ttss.repository.AuditTtssRecordRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Gan vao end event "endRejected" cua quy trinh audit_recommendation_approval - danh cho tuong lai
 * khi co buoc duyet co the tu choi (hien tai luon complete voi approved=true nen nhanh nay chua
 * duoc kich hoat, nhung BPMN/listener san sang san).
 */
@Component
public class AuditRecommendationApprovalRejectedListener implements ExecutionListener {

    private final AuditTtssRecordRepository ttssRecordRepository;
    private final AuditLogService auditLogService;

    public AuditRecommendationApprovalRejectedListener(AuditTtssRecordRepository ttssRecordRepository, AuditLogService auditLogService) {
        this.ttssRecordRepository = ttssRecordRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public void notify(DelegateExecution execution) {
        Object ttssRecordIdValue = execution.getVariable("ttssRecordId");
        if (ttssRecordIdValue == null) {
            return;
        }
        UUID ttssRecordId = UUID.fromString(ttssRecordIdValue.toString());

        ttssRecordRepository.findById(ttssRecordId).ifPresent(record -> {
            record.setRecommendationApprovalStatus(AssignmentApprovalStatus.REJECTED);
            ttssRecordRepository.save(record);
            auditLogService.record("AuditTtssRecord", ttssRecordId, AuditAction.REJECT,
                    "Tu choi phe duyet kien nghi qua quy trinh phe duyet (audit_recommendation_approval)");
        });
    }
}
