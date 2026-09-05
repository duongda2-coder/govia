package com.govia.identity.service;

import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.tenant.TenantContext;
import com.govia.identity.entity.ApprovalMatrixRule;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.EmployeeRankLevel;
import com.govia.identity.workflow.approval.ManagerHierarchyApprovalChainResolver;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cau noi giua nghiep vu Employee va quy trinh Flowable "employee_approval" (xem
 * employee-approval.bpmn20.xml). Chi EmployeeService goi vao day - khong lam nguoc lai (workflow
 * package khong phu thuoc People) de tranh phu thuoc vong.
 *
 * Dây phe duyet la DONG: do dai phu thuoc "ma tran phe duyet" (ApprovalMatrixRule) cau hinh cho tung
 * don vi to chuc (hoac quy tac mac dinh) - khong con gioi han cung 3 cap nhu ban truoc.
 */
@Service
public class EmployeeApprovalService {

    private static final String PROCESS_KEY = "employee_approval";
    /** Neu chua cau hinh ApprovalMatrixRule nao (tenant moi/chua thiet lap), dung fallback an toan
     * nay: di het chuoi quan ly toi N6 va LUON co them buoc Super Admin. */
    private static final EmployeeRankLevel FALLBACK_FINAL_LEVEL = EmployeeRankLevel.N6;
    private static final boolean FALLBACK_REQUIRE_FINAL_STEP = true;

    private final ManagerHierarchyApprovalChainResolver chainResolver;
    private final ApprovalMatrixRuleService approvalMatrixRuleService;
    private final RuntimeService runtimeService;
    private final AuditLogService auditLogService;

    public EmployeeApprovalService(ManagerHierarchyApprovalChainResolver chainResolver,
                                    ApprovalMatrixRuleService approvalMatrixRuleService, RuntimeService runtimeService,
                                    AuditLogService auditLogService) {
        this.chainResolver = chainResolver;
        this.approvalMatrixRuleService = approvalMatrixRuleService;
        this.runtimeService = runtimeService;
        this.auditLogService = auditLogService;
    }

    /**
     * Goi ngay sau khi luu 1 Employee moi co status PENDING_APPROVAL (employee.getManagerId() != null).
     * Tra cuu ma tran phe duyet theo don vi to chuc cua nhan vien, di nguoc chuoi quan ly toi khi dat
     * nguong cap bac cau hinh, roi start quy trinh Flowable voi dây duyet dong tuong ung.
     */
    public void startApprovalIfNeeded(Employee employee) {
        if (employee.getManagerId() == null) {
            return;
        }

        UUID tenantId = TenantContext.getTenantId();
        ApprovalMatrixRule rule = approvalMatrixRuleService.resolveActiveRuleForOrgUnit(tenantId, employee.getOrgUnitId());
        EmployeeRankLevel finalLevel = rule != null ? rule.getFinalApprovalLevel() : FALLBACK_FINAL_LEVEL;
        boolean requireFinalStep = rule != null ? rule.isRequireFinalSuperAdminStep() : FALLBACK_REQUIRE_FINAL_STEP;

        List<UUID> approverUserIds = chainResolver.resolveChain(employee.getManagerId(), finalLevel);

        Map<String, Object> variables = new HashMap<>();
        variables.put("employeeId", employee.getId().toString());
        // Mac dinh true de xu ly dung khi approverChain rong (Multi-Instance 0 vong lap - xem BPMN).
        variables.put("approved", true);
        variables.put("approverChain", approverUserIds.stream().map(UUID::toString).toList());
        variables.put("requireFinalStep", requireFinalStep);

        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey(PROCESS_KEY)
                .tenantId(tenantId.toString())
                .businessKey(employee.getEmployeeCode())
                .variables(variables)
                .start();

        auditLogService.record("Employee", employee.getId(), AuditAction.CREATE,
                "Bat dau quy trinh phe duyet nhan vien " + employee.getEmployeeCode()
                        + " (" + approverUserIds.size() + " nguoi duyet theo day quan ly"
                        + (requireFinalStep ? " + Super Admin" : "") + ", nguong cap=" + finalLevel + ")");
    }
}
