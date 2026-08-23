package com.govia.identity.workflow.delegate;

import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.EmployeeStatus;
import com.govia.identity.repository.EmployeeRepository;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Gan vao end event "endApproved" cua quy trinh employee_approval (xem employee-approval.bpmn20.xml):
 * khi toan bo cac cap duyet deu approved, chuyen Employee tu PENDING_APPROVAL sang ACTIVE.
 */
@Component
public class EmployeeApprovalApprovedListener implements ExecutionListener {

    private final EmployeeRepository employeeRepository;
    private final AuditLogService auditLogService;

    public EmployeeApprovalApprovedListener(EmployeeRepository employeeRepository, AuditLogService auditLogService) {
        this.employeeRepository = employeeRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public void notify(DelegateExecution execution) {
        Object employeeIdValue = execution.getVariable("employeeId");
        if (employeeIdValue == null) {
            return;
        }
        UUID employeeId = UUID.fromString(employeeIdValue.toString());
        employeeRepository.findById(employeeId).ifPresent(employee -> {
            employee.setStatus(EmployeeStatus.ACTIVE);
            employeeRepository.save(employee);
            auditLogService.record("Employee", employee.getId(), AuditAction.APPROVE,
                    "Duyet nhan vien " + employee.getEmployeeCode() + " qua quy trinh phe duyet (employee_approval)");
        });
    }
}
