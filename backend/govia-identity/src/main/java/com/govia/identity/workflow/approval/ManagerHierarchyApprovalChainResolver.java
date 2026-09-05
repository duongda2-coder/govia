package com.govia.identity.workflow.approval;

import com.govia.identity.entity.Employee;
import com.govia.identity.entity.EmployeeRankLevel;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Ham "get dây phê duyệt" DUNG CHUNG: di nguoc chuoi quan ly (Employee.managerId) bat dau tu 1
 * employee, toi khi gap nguoi co rank_level >= nguong cau hinh thi dung lai. Tach ra tu
 * EmployeeApprovalService de bat ky domain nao khac (vd audit work-item, expense...) can dây
 * phe duyet theo so do to chuc deu goi lai duoc component nay, khong phai chep lai logic.
 */
@Component
public class ManagerHierarchyApprovalChainResolver {

    /** Chan tren so cap TO TIEN duoc di qua - tranh di het toan bo so do to chuc neu cau hinh/du
     * lieu bat thuong (vd vong lap). */
    private static final int MAX_ANCESTORS_WALKED = 20;

    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;

    public ManagerHierarchyApprovalChainResolver(EmployeeRepository employeeRepository, UserAccountRepository userAccountRepository) {
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * Di nguoc tu managerId (Employee.id): voi moi to tien, neu ho co UserAccount thi them vao dây
     * duyet (khong co tai khoan thi khong the giao task duyet duoc, bo qua nhung van di tiep len
     * tren de kiem tra nguong); dung lai ngay khi gap nguoi co rank_level >= finalLevel (bat ke
     * nguoi do co tai khoan hay khong - day la "diem dung" theo cau hinh).
     */
    public List<UUID> resolveChain(UUID firstManagerId, EmployeeRankLevel finalLevel) {
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
