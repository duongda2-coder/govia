package com.govia.audit.planengagement.ttss.workflow;

import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.audit.planengagement.ttss.repository.AuditTtssRecordRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Gan vao end event "endApproved" cua quy trinh audit_recommendation_approval (xem
 * audit-recommendation-approval.bpmn20.xml): khi day duyet dat, chuyen
 * AuditTtssRecord.recommendationApprovalStatus sang APPROVED.
 */
@Component
public class AuditRecommendationApprovalApprovedListener implements ExecutionListener {

    private final AuditTtssRecordRepository ttssRecordRepository;
    private final AuditLogService auditLogService;

    public AuditRecommendationApprovalApprovedListener(AuditTtssRecordRepository ttssRecordRepository, AuditLogService auditLogService) {
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
        Object approverUsername = execution.getVariable("approverUsername");

        ttssRecordRepository.findById(ttssRecordId).ifPresent(record -> {
            record.setRecommendationApprovalStatus(AssignmentApprovalStatus.APPROVED);
            record.setRecommendationApprovedBy(approverUsername == null ? null : approverUsername.toString());
            record.setRecommendationApprovedAt(Instant.now());
            ttssRecordRepository.save(record);
            auditLogService.record("AuditTtssRecord", ttssRecordId, AuditAction.APPROVE,
                    "Truong doan phe duyet kien nghi qua quy trinh phe duyet (audit_recommendation_approval)");
        });
    }
}
