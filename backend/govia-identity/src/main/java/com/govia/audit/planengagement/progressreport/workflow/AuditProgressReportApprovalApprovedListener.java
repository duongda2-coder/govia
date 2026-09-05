package com.govia.audit.planengagement.progressreport.workflow;

import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.audit.planengagement.progressreport.repository.AuditProgressReportRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Gan vao end event "endApproved" cua quy trinh audit_progress_report_approval (xem
 * audit-progress-report-approval.bpmn20.xml): khi day duyet dat, chuyen AuditProgressReport sang
 * APPROVED.
 */
@Component
public class AuditProgressReportApprovalApprovedListener implements ExecutionListener {

    private final AuditProgressReportRepository progressReportRepository;
    private final AuditLogService auditLogService;

    public AuditProgressReportApprovalApprovedListener(AuditProgressReportRepository progressReportRepository, AuditLogService auditLogService) {
        this.progressReportRepository = progressReportRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public void notify(DelegateExecution execution) {
        Object reportIdValue = execution.getVariable("progressReportId");
        if (reportIdValue == null) {
            return;
        }
        UUID reportId = UUID.fromString(reportIdValue.toString());
        Object approverUsername = execution.getVariable("approverUsername");

        progressReportRepository.findById(reportId).ifPresent(report -> {
            report.setApprovalStatus(AssignmentApprovalStatus.APPROVED);
            report.setApprovedBy(approverUsername == null ? null : approverUsername.toString());
            report.setApprovedAt(Instant.now());
            progressReportRepository.save(report);
            auditLogService.record("AuditProgressReport", reportId, AuditAction.APPROVE,
                    "Truong doan phe duyet bao cao tien do qua quy trinh phe duyet (audit_progress_report_approval)");
        });
    }
}
