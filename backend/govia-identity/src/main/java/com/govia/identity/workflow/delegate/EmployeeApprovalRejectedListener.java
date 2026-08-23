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
 * Gan vao end event "endRejected" cua quy trinh employee_approval (xem employee-approval.bpmn20.xml):
 * bat ky cap nao tu choi, chuyen Employee tu PENDING_APPROVAL sang REJECTED.
 */
@Component
public class EmployeeApprovalRejectedListener implements ExecutionListener {

    private final EmployeeRepository employeeRepository;
    private final AuditLogService auditLogService;

    public EmployeeApprovalRejectedListener(EmployeeRepository employeeRepository, AuditLogService auditLogService) {
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
            employee.setStatus(EmployeeStatus.REJECTED);
            employeeRepository.save(employee);
            auditLogService.record("Employee", employee.getId(), AuditAction.REJECT,
                    "Tu choi nhan vien " + employee.getEmployeeCode() + " qua quy trinh phe duyet (employee_approval)");
        });
    }
}
