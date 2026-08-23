package com.govia.identity.service;

import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.tenant.TenantContext;
import com.govia.identity.entity.ApprovalMatrixRule;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.EmployeeRankLevel;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.repository.UserAccountRepository;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    /** Chan tren so cap TO TIEN duoc di qua (bao gom ca nguoi bi bo qua vi chua co tai khoan dang
     * nhap) - tranh di het toan bo so do to chuc neu cau hinh/du lieu bat thuong (vd vong lap). */
    private static final int MAX_ANCESTORS_WALKED = 20;

    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final ApprovalMatrixRuleService approvalMatrixRuleService;
    private final RuntimeService runtimeService;
    private final AuditLogService auditLogService;

    public EmployeeApprovalService(EmployeeRepository employeeRepository, UserAccountRepository userAccountRepository,
                                    ApprovalMatrixRuleService approvalMatrixRuleService, RuntimeService runtimeService,
                                    AuditLogService auditLogService) {
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
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

        List<UUID> approverUserIds = resolveDynamicApproverChain(employee.getManagerId(), finalLevel);

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

    /**
     * Di nguoc tu managerId (Employee.id): voi moi to tien, neu ho co UserAccount thi them vao dây
     * duyet (khong co tai khoan thi khong the giao task duyet duoc, bo qua nhung van di tiep len tren
     * de kiem tra nguong); dung lai ngay khi gap nguoi co rank_level >= finalLevel (bat ke nguoi do co
     * tai khoan hay khong - day la "diem dung" theo cau hinh).
     */
    private List<UUID> resolveDynamicApproverChain(UUID firstManagerId, EmployeeRankLevel finalLevel) {
        List<UUID> approvers = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        UUID cursor = firstManagerId;
        int walked = 0;

        while (cursor != null && walked < MAX_ANCESTORS_WALKED) {
            if (!visited.add(cursor)) {
                break;
            }
            walked++;

            Employee managerEmployee = employeeRepository.findById(cursor).orElse(null);
            if (managerEmployee == null) {
                break;
            }

            userAccountRepository.findByEmployeeId(cursor).ifPresent(account -> approvers.add(account.getId()));

            boolean reachedThreshold = managerEmployee.getRankLevel() != null
                    && managerEmployee.getRankLevel().ordinal() >= finalLevel.ordinal();
            if (reachedThreshold) {
                break;
            }
            cursor = managerEmployee.getManagerId();
        }
        return approvers;
    }
}
