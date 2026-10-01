package com.govia.audit.khkt.khnsnam.service;

import com.govia.audit.khkt.khnsnam.dto.AuditKhnsTransferCandidateResponse;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsTransferResultItem;
import com.govia.audit.khkt.khnsnam.entity.AuditKhnsNam;
import com.govia.audit.khkt.khnsnam.entity.AuditKhnsNamObject;
import com.govia.audit.khkt.khnsnam.entity.AuditKhnsRoleInTeam;
import com.govia.audit.khkt.khnsnam.repository.AuditKhnsNamObjectRepository;
import com.govia.audit.khkt.khnsnam.repository.AuditKhnsNamRepository;
import com.govia.audit.khkt.th.entity.AuditKhktThConfirmed;
import com.govia.audit.khkt.th.repository.AuditKhktThConfirmedRepository;
import com.govia.audit.planengagement.dto.AuditEngagementRequest;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.entity.AuditEngagementStatus;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.service.AuditEngagementService;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.core.tenant.TenantContext;
import com.govia.identity.entity.Employee;
import com.govia.identity.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** "Chuyển thông tin KHTH" (sheet ChuyenthongtinKHTH) - day du lieu KHNS_NAM (nhan su + doi tuong +
 * thang + so/ngay quyet dinh, cac gia tri nay khong bat buoc) sang AuditEngagement ("ztc_job", man hinh khoi tao CKT). Chon tung
 * doi tuong kiem toan roi bam chuyen (theo yeu cau NSD) - moi doi tuong ung 1 CKT, ghi de neu da
 * ton tai (dung dac ta "mỗi năm, một đối tượng kiểm toán chỉ có một mã cuộc kiểm toán"). */
@Service
public class AuditKhktTransferService {

    private final AuditKhnsNamRepository khnsNamRepository;
    private final AuditKhnsNamObjectRepository khnsNamObjectRepository;
    private final AuditKhktThConfirmedRepository thConfirmedRepository;
    private final AuditObjectUnitRepository objectUnitRepository;
    private final AuditEngagementRepository engagementRepository;
    private final AuditEngagementService engagementService;
    private final EmployeeRepository employeeRepository;

    public AuditKhktTransferService(AuditKhnsNamRepository khnsNamRepository, AuditKhnsNamObjectRepository khnsNamObjectRepository,
                                     AuditKhktThConfirmedRepository thConfirmedRepository, AuditObjectUnitRepository objectUnitRepository,
                                     AuditEngagementRepository engagementRepository, AuditEngagementService engagementService,
                                     EmployeeRepository employeeRepository) {
        this.khnsNamRepository = khnsNamRepository;
        this.khnsNamObjectRepository = khnsNamObjectRepository;
        this.thConfirmedRepository = thConfirmedRepository;
        this.objectUnitRepository = objectUnitRepository;
        this.engagementRepository = engagementRepository;
        this.engagementService = engagementService;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditKhnsTransferCandidateResponse> listCandidates(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditKhktThConfirmed> thRows = thConfirmedRepository.findByTenantIdAndYearOrderByAuditObjectCodeAsc(tenantId, year);
        TeamLeadIndex index = buildTeamLeadIndex(tenantId, year);
        Map<String, AuditObjectUnit> unitsByCode = objectUnitRepository.findAllById(
                        thRows.stream().map(AuditKhktThConfirmed::getAuditObjectUnitId).filter(java.util.Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(AuditObjectUnit::getCode, u -> u, (a, b) -> a));

        return thRows.stream().map(row -> toCandidate(tenantId, year, row, index, unitsByCode)).toList();
    }

    @Transactional
    public List<AuditKhnsTransferResultItem> transfer(Integer year, List<String> auditObjectCodes) {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditKhktThConfirmed> thRows = thConfirmedRepository.findByTenantIdAndYearOrderByAuditObjectCodeAsc(tenantId, year);
        Map<String, AuditKhktThConfirmed> thByCode = thRows.stream()
                .collect(Collectors.toMap(AuditKhktThConfirmed::getAuditObjectCode, r -> r, (a, b) -> a));
        TeamLeadIndex index = buildTeamLeadIndex(tenantId, year);
        Map<String, AuditObjectUnit> unitsByCode = objectUnitRepository.findAllById(
                        thRows.stream().map(AuditKhktThConfirmed::getAuditObjectUnitId).filter(java.util.Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(AuditObjectUnit::getCode, u -> u, (a, b) -> a));

        List<AuditKhnsTransferResultItem> results = new ArrayList<>();
        for (String code : auditObjectCodes) {
            AuditKhktThConfirmed thRow = thByCode.get(code);
            if (thRow == null) {
                results.add(new AuditKhnsTransferResultItem(code, false, null, "Doi tuong khong thuoc danh sach TH2 cua nam " + year));
                continue;
            }
            AuditKhnsTransferCandidateResponse candidate = toCandidate(tenantId, year, thRow, index, unitsByCode);
            if (!candidate.transferable()) {
                results.add(new AuditKhnsTransferResultItem(code, false, null, candidate.blockReason()));
                continue;
            }
            try {
                String engagementCode = pushOne(tenantId, year, thRow, candidate, unitsByCode.get(code));
                results.add(new AuditKhnsTransferResultItem(code, true, engagementCode, null));
            } catch (Exception e) {
                results.add(new AuditKhnsTransferResultItem(code, false, null, e.getMessage()));
            }
        }
        return results;
    }

    private String pushOne(UUID tenantId, Integer year, AuditKhktThConfirmed thRow, AuditKhnsTransferCandidateResponse candidate,
                            AuditObjectUnit unit) {
        Employee teamLead = employeeRepository.findByTenantIdAndEmployeeCode(tenantId, candidate.teamLeadEmployeeCode())
                .orElseThrow(() -> new IllegalStateException("Khong tim thay can bo Truong doan: " + candidate.teamLeadEmployeeCode()));

        // test 10.1: moi lan chuyen la mot CKT MOI (vd da co CN1300202601 -> tao CN1300202602), khong ghi de CKT da co cua
        // cung doi tuong/nam - CKT cu co the dang thuc hien (nhom, TTSS...), ghi de lam mat/lan du lieu.
        AuditEngagementRequest request = new AuditEngagementRequest(unit.getId(), year, candidate.expectedMonth(), candidate.decisionDate(),
                teamLead.getId(), candidate.decisionNumber() != null && !candidate.decisionNumber().isBlank() ? candidate.decisionNumber() : null,
                AuditEngagementStatus.DRAFT, thRow.getRankLabel(),
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        return engagementService.create(request).code();
    }

    private AuditKhnsTransferCandidateResponse toCandidate(UUID tenantId, Integer year, AuditKhktThConfirmed thRow, TeamLeadIndex index,
                                                             Map<String, AuditObjectUnit> unitsByCode) {
        String code = thRow.getAuditObjectCode();
        TeamLeadInfo info = index.byObjectCode().get(code);
        AuditObjectUnit unit = unitsByCode.get(code);

        if (unit == null) {
            return blocked(thRow, info, "Doi tuong chua co trong danh muc AuditObjectUnit (khong xac dinh duoc de tao CKT)");
        }
        if (info == null) {
            return blocked(thRow, null, "Chua co Truong doan duoc phan cong cho doi tuong nay o KHNS_NAM");
        }
        // Thang du kien / So QD / Ngay QD KHONG bat buoc: thieu thi van chuyen, CKT de trong cac cot do (nhap bo sung sau o man CKT)

        String existingEngagementCode = engagementRepository
                .findFirstByTenantIdAndAuditObjectUnitIdAndYearOrderByCreatedAtAsc(tenantId, unit.getId(), year)
                .map(AuditEngagement::getCode).orElse(null);

        return new AuditKhnsTransferCandidateResponse(code, thRow.getAuditObjectName(), info.employeeCode(), info.employeeName(),
                info.memberCount(), info.expectedMonth(), info.decisionNumber(), info.decisionDate(), info.expectedBatch(),
                existingEngagementCode, true, null);
    }

    private AuditKhnsTransferCandidateResponse blocked(AuditKhktThConfirmed thRow, TeamLeadInfo info, String reason) {
        return new AuditKhnsTransferCandidateResponse(thRow.getAuditObjectCode(), thRow.getAuditObjectName(),
                info == null ? null : info.employeeCode(), info == null ? null : info.employeeName(),
                info == null ? 0 : info.memberCount(), info == null ? null : info.expectedMonth(),
                info == null ? null : info.decisionNumber(), info == null ? null : info.decisionDate(),
                info == null ? null : info.expectedBatch(), null, false, reason);
    }

    private record TeamLeadInfo(String employeeCode, String employeeName, int memberCount, Integer expectedMonth,
                                 String decisionNumber, java.time.LocalDate decisionDate, String expectedBatch) {
    }

    private record TeamLeadIndex(Map<String, TeamLeadInfo> byObjectCode) {
    }

    /** Gom theo doi tuong kiem toan: dem so nhan vien tham chieu toi doi tuong do (memberCount), va
     * lay rieng thong tin cua nguoi duoc danh dau TEAM_LEAD (da dam bao duy nhat qua validation luc
     * luu KHNS_NAM - xem AuditKhnsNamService#validateNoTeamLeadConflict). */
    private TeamLeadIndex buildTeamLeadIndex(UUID tenantId, Integer year) {
        List<AuditKhnsNam> plans = khnsNamRepository.findByTenantIdAndYear(tenantId, year);
        if (plans.isEmpty()) {
            return new TeamLeadIndex(Map.of());
        }
        Map<UUID, List<String>> objectCodesByPlan = new HashMap<>();
        for (AuditKhnsNamObject obj : khnsNamObjectRepository.findByTenantIdAndKhnsNamIdIn(tenantId, plans.stream().map(AuditKhnsNam::getId).toList())) {
            objectCodesByPlan.computeIfAbsent(obj.getKhnsNamId(), k -> new ArrayList<>()).add(obj.getAuditObjectCode());
        }
        Map<UUID, Employee> employeesById = employeeRepository.findAllById(plans.stream().map(AuditKhnsNam::getEmployeeId).toList())
                .stream().collect(Collectors.toMap(Employee::getId, e -> e));

        Map<String, Integer> memberCounts = new HashMap<>();
        Map<String, TeamLeadInfo> byObjectCode = new HashMap<>();
        for (AuditKhnsNam plan : plans) {
            Map<Integer, String> monthCodes = monthCodesOf(plan);
            java.util.Set<String> referenced = new java.util.HashSet<>(objectCodesByPlan.getOrDefault(plan.getId(), List.of()));
            referenced.addAll(monthCodes.values());
            for (String code : referenced) {
                if (code == null || code.isBlank()) {
                    continue;
                }
                memberCounts.merge(code, 1, Integer::sum);
                if (plan.getRoleInTeam() == AuditKhnsRoleInTeam.TEAM_LEAD) {
                    Employee employee = employeesById.get(plan.getEmployeeId());
                    Integer expectedMonth = monthCodes.entrySet().stream()
                            .filter(e -> code.equals(e.getValue())).map(Map.Entry::getKey).findFirst().orElse(null);
                    byObjectCode.put(code, new TeamLeadInfo(employee == null ? null : employee.getEmployeeCode(),
                            employee == null ? null : employee.getFullName(), 0, expectedMonth, plan.getDecisionNumber(),
                            plan.getDecisionDate(), plan.getExpectedBatch()));
                }
            }
        }
        Map<String, TeamLeadInfo> withCounts = new HashMap<>();
        byObjectCode.forEach((code, info) -> withCounts.put(code, new TeamLeadInfo(info.employeeCode(), info.employeeName(),
                memberCounts.getOrDefault(code, 0), info.expectedMonth(), info.decisionNumber(), info.decisionDate(), info.expectedBatch())));
        return new TeamLeadIndex(withCounts);
    }

    private Map<Integer, String> monthCodesOf(AuditKhnsNam plan) {
        Map<Integer, String> result = new HashMap<>();
        result.put(1, plan.getMonth1AuditObjectCode());
        result.put(2, plan.getMonth2AuditObjectCode());
        result.put(3, plan.getMonth3AuditObjectCode());
        result.put(4, plan.getMonth4AuditObjectCode());
        result.put(5, plan.getMonth5AuditObjectCode());
        result.put(6, plan.getMonth6AuditObjectCode());
        result.put(7, plan.getMonth7AuditObjectCode());
        result.put(8, plan.getMonth8AuditObjectCode());
        result.put(9, plan.getMonth9AuditObjectCode());
        result.put(10, plan.getMonth10AuditObjectCode());
        result.put(11, plan.getMonth11AuditObjectCode());
        result.put(12, plan.getMonth12AuditObjectCode());
        return result;
    }
}
