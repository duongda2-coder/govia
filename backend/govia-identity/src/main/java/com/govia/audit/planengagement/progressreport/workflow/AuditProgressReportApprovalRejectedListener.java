package com.govia.audit.planengagement.progressreport.workflow;

import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.audit.planengagement.progressreport.repository.AuditProgressReportRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Gan vao end event "endRejected" cua quy trinh audit_progress_report_approval - danh cho tuong
 * lai khi co buoc duyet co the tu choi (hien tai luon complete voi approved=true nen nhanh nay
 * chua duoc kich hoat, nhung BPMN/listener san sang san).
 */
@Component
public class AuditProgressReportApprovalRejectedListener implements ExecutionListener {

    private final AuditProgressReportRepository progressReportRepository;
    private final AuditLogService auditLogService;

    public AuditProgressReportApprovalRejectedListener(AuditProgressReportRepository progressReportRepository, AuditLogService auditLogService) {
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

        progressReportRepository.findById(reportId).ifPresent(report -> {
            report.setApprovalStatus(AssignmentApprovalStatus.REJECTED);
            progressReportRepository.save(report);
            auditLogService.record("AuditProgressReport", reportId, AuditAction.REJECT,
                    "Tu choi phe duyet bao cao tien do qua quy trinh phe duyet (audit_progress_report_approval)");
        });
    }
}
