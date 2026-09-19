package com.govia.audit.khkt.khnsnam.service;

import com.govia.audit.employeecapability.entity.AuditEmployeeCapability;
import com.govia.audit.employeecapability.repository.AuditEmployeeCapabilityRepository;
import com.govia.audit.khkt.khnsnam.allocation.AuditKhnsPbAllocator;
import com.govia.audit.khkt.khnsnam.allocation.AuditKhnsPbAllocator.ScaleLevel;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsPbAllocationResult;
import com.govia.audit.khkt.khnsnam.entity.AuditKhnsNam;
import com.govia.audit.khkt.khnsnam.entity.AuditKhnsNamObject;
import com.govia.audit.khkt.khnsnam.entity.AuditKhnsRoleInTeam;
import com.govia.audit.khkt.khnsnam.repository.AuditKhnsNamObjectRepository;
import com.govia.audit.khkt.khnsnam.repository.AuditKhnsNamRepository;
import com.govia.audit.khkt.scale.dto.AuditKhktScaleResponse;
import com.govia.audit.khkt.scale.service.AuditKhktScaleService;
import com.govia.audit.khkt.thang.dto.AuditKhktThangRowResponse;
import com.govia.audit.khkt.thang.service.AuditKhktThangService;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.planengagement.service.AuditEngagementService;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.tenant.TenantContext;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.EmployeeStatus;
import com.govia.identity.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/** Nut "Phan bo nhan su" o KHNS_PB: phan bo can bo cho CA NAM dua tren "Khai bao so thang kiem toan
 * trong nam" (KHKT_THANG - chi doi tuong da phe duyet o TH2) va nguyen tac bo tri nhan su 1 doan kiem
 * toan (xem {@link AuditKhnsPbAllocator}). Ghi de toan bo phan bo thang/doi tuong cua nam duoc chon
 * trong KHNS_NAM; giu nguyen cac truong khac (so/ngay quyet dinh, dot, ghi chu, cong viec khac).
 *
 * <p>Gia dinh khi dac ta khong ro (xem test18.9):
 * <ul>
 *   <li>Quy mo nghiep vu: Tin dung theo "Quy mo tin dung", HDV theo "Quy mo huy dong von" (KHKT_QM);
 *       "trung binh tro len" = muc quy mo cao hon muc thap nhat, "lon" = muc cao nhat. Cac nghiep vu
 *       con lai chua co phu luc phan loai quy mo nen coi la trung binh/thap.</li>
 *   <li>"Don vi minh tung lam viec (3 nam)": doi chieu ten don vi voi "Qua trinh cong tac truoc day"
 *       cua can bo (loai cung); "Don vi co nguoi lien quan": doi chieu voi "Chi nhanh co nguoi lien
 *       quan" (loai cung). "Chi nhanh da tham gia kiem toan nam truoc" chi bi tru diem (xoay vong).</li>
 *   <li>Can bo chua co "Phan loai nang luc KTV" duoc coi la bac 1. Can bo nghi (ON_LEAVE/
 *       Trang thai khac ACTIVE) khong duoc phan bo.</li>
 *   <li>"Bo tri them theo quy mo/ket qua ra soat" la tuy chon theo dac ta nen chua tu dong ap dung.</li>
 * </ul>
 */
@Service
public class AuditKhnsPbService {

    private final AuditKhktThangService thangService;
    private final AuditKhktScaleService scaleService;
    private final EmployeeRepository employeeRepository;
    private final AuditEmployeeCapabilityRepository capabilityRepository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditKhnsNamRepository khnsNamRepository;
    private final AuditKhnsNamObjectRepository khnsNamObjectRepository;
    private final AuditLogService auditLogService;

    public AuditKhnsPbService(AuditKhktThangService thangService, AuditKhktScaleService scaleService, EmployeeRepository employeeRepository,
                               AuditEmployeeCapabilityRepository capabilityRepository, AuditMasterDataItemRepository masterDataItemRepository,
                               AuditKhnsNamRepository khnsNamRepository, AuditKhnsNamObjectRepository khnsNamObjectRepository,
                               AuditLogService auditLogService) {
        this.thangService = thangService;
        this.scaleService = scaleService;
        this.employeeRepository = employeeRepository;
        this.capabilityRepository = capabilityRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.khnsNamRepository = khnsNamRepository;
        this.khnsNamObjectRepository = khnsNamObjectRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AuditKhnsPbAllocationResult allocate(Integer year) {
        UUID tenantId = TenantContext.getTenantId();

        List<AuditKhktThangRowResponse> thangRows = thangService.list(year).stream().filter(r -> !monthsOf(r).isEmpty()).toList();
        if (thangRows.isEmpty()) {
            return new AuditKhnsPbAllocationResult(0, 0, 0, List.of(
                    "Chưa có đối tượng kiểm toán nào được khai báo số tháng kiểm toán trong năm " + year
                            + " (chỉ tính các đối tượng đã phê duyệt ở Danh sách đối tượng kiểm toán năm của Phòng Kế hoạch - Phần 2)."));
        }

        List<AuditKhktScaleResponse> scales = scaleService.list().stream().filter(AuditKhktScaleResponse::active).toList();
        List<AuditKhnsPbAllocator.TeamObject> objects = thangRows.stream().map(r -> new AuditKhnsPbAllocator.TeamObject(
                r.auditObjectCode(), r.auditObjectName(), monthsOf(r), normalizeSegmentCodes(r.businessSegmentCodes()),
                levelOf(r.creditScale(), scales), levelOf(r.fundingScale(), scales))).toList();

        AuditKhnsPbAllocator.Result result = new AuditKhnsPbAllocator().allocate(objects, buildStaff(tenantId, thangRows));
        persist(tenantId, year, result.assignments());

        auditLogService.record("AuditKhnsNam", null, AuditAction.UPDATE,
                "Phan bo nhan su ca nam KHKT " + year + ": " + result.assignments().size() + " can bo, "
                        + result.objectsFullyStaffed() + "/" + result.objectCount() + " doan du dinh bien");
        return new AuditKhnsPbAllocationResult(result.objectCount(), result.objectsFullyStaffed(), result.assignments().size(), result.warnings());
    }

    private List<AuditKhnsPbAllocator.Staff> buildStaff(UUID tenantId, List<AuditKhktThangRowResponse> thangRows) {
        Map<UUID, AuditEmployeeCapability> capabilities = capabilityRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(AuditEmployeeCapability::getEmployeeId, c -> c, (a, b) -> a));
        Map<UUID, String> segmentCodeById = masterDataItemRepository
                .findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT).stream()
                .collect(Collectors.toMap(AuditMasterDataItem::getId, AuditMasterDataItem::getCode, (a, b) -> a));

        List<AuditKhnsPbAllocator.Staff> staff = new ArrayList<>();
        for (Employee e : employeeRepository.findByTenantIdOrderByFullNameAsc(tenantId)) {
            if (e.getStatus() != EmployeeStatus.ACTIVE || e.isOnLeave()) {
                continue;
            }
            AuditEmployeeCapability capability = capabilities.get(e.getId());
            Set<String> capable = new HashSet<>(AuditEngagementService.capableSegmentCodes(capability));
            boolean leadCapable = e.isTeamLeadCapable() || (capability != null && capability.isTruongDoanCapable());
            if (capable.isEmpty() && !leadCapable) {
                continue;
            }
            Set<String> related = tokens(e.getRelatedPersonBranches());
            Set<String> audited = tokens(e.getAuditedBranches());
            String priorWork = normalize(e.getPriorWorkHistory());

            Set<String> blocked = new HashSet<>();
            Set<String> rotated = new HashSet<>();
            for (AuditKhktThangRowResponse row : thangRows) {
                String name = normalize(row.auditObjectName());
                String code = normalize(row.auditObjectCode());
                if (related.contains(name) || related.contains(code) || (!name.isEmpty() && priorWork.contains(name))) {
                    blocked.add(row.auditObjectCode());
                }
                if (audited.contains(name) || audited.contains(code)) {
                    rotated.add(row.auditObjectCode());
                }
            }
            int grade = e.getAuditorClassification() == null ? 1 : e.getAuditorClassification().ordinal() + 1;
            staff.add(new AuditKhnsPbAllocator.Staff(e.getId(), e.getEmployeeCode(), e.getFullName(), grade, capable, leadCapable,
                    ownSegmentCode(e, segmentCodeById), blocked, rotated));
        }
        return staff;
    }

    private String ownSegmentCode(Employee e, Map<UUID, String> segmentCodeById) {
        String code = e.getBusinessSegmentId() == null ? null : segmentCodeById.get(e.getBusinessSegmentId());
        return "TD".equals(code) ? AuditKhnsPbAllocator.CREDIT : code;
    }

    private void persist(UUID tenantId, Integer year, Map<UUID, AuditKhnsPbAllocator.StaffAssignment> assignments) {
        Map<UUID, AuditKhnsNam> plansByEmployee = khnsNamRepository.findByTenantIdAndYear(tenantId, year).stream()
                .collect(Collectors.toMap(AuditKhnsNam::getEmployeeId, p -> p));

        // Phan bo lai ca nam: xoa phan bo thang/doi tuong cu cua moi can bo roi ghi phan bo moi
        Set<UUID> employeeIds = new HashSet<>(plansByEmployee.keySet());
        employeeIds.addAll(assignments.keySet());
        for (UUID employeeId : employeeIds) {
            AuditKhnsPbAllocator.StaffAssignment assignment = assignments.get(employeeId);
            AuditKhnsNam plan = plansByEmployee.get(employeeId);
            if (plan == null) {
                plan = new AuditKhnsNam();
                plan.setTenantId(tenantId);
                plan.setYear(year);
                plan.setEmployeeId(employeeId);
            }
            for (int month = 1; month <= 12; month++) {
                setMonth(plan, month, assignment == null ? null : assignment.getMonthToObject().get(month));
            }
            if (assignment != null) {
                if (assignment.isLead()) {
                    plan.setRoleInTeam(AuditKhnsRoleInTeam.TEAM_LEAD);
                } else if (plan.getRoleInTeam() == null || plan.getRoleInTeam() == AuditKhnsRoleInTeam.TEAM_LEAD) {
                    plan.setRoleInTeam(AuditKhnsRoleInTeam.MEMBER);
                }
            }
            plan = khnsNamRepository.save(plan);

            khnsNamObjectRepository.deleteByTenantIdAndKhnsNamId(tenantId, plan.getId());
            if (assignment != null) {
                for (String code : assignment.getObjectCodes()) {
                    AuditKhnsNamObject obj = new AuditKhnsNamObject();
                    obj.setTenantId(tenantId);
                    obj.setKhnsNamId(plan.getId());
                    obj.setAuditObjectCode(code);
                    khnsNamObjectRepository.save(obj);
                }
            }
        }
    }

    private void setMonth(AuditKhnsNam plan, int month, String code) {
        switch (month) {
            case 1 -> plan.setMonth1AuditObjectCode(code);
            case 2 -> plan.setMonth2AuditObjectCode(code);
            case 3 -> plan.setMonth3AuditObjectCode(code);
            case 4 -> plan.setMonth4AuditObjectCode(code);
            case 5 -> plan.setMonth5AuditObjectCode(code);
            case 6 -> plan.setMonth6AuditObjectCode(code);
            case 7 -> plan.setMonth7AuditObjectCode(code);
            case 8 -> plan.setMonth8AuditObjectCode(code);
            case 9 -> plan.setMonth9AuditObjectCode(code);
            case 10 -> plan.setMonth10AuditObjectCode(code);
            case 11 -> plan.setMonth11AuditObjectCode(code);
            default -> plan.setMonth12AuditObjectCode(code);
        }
    }

    /** Danh muc Nghiep vu chuan dung ma "LN" cho Tin dung (xem AuditEngagementService.capableSegmentCodes), nhung
     * 1 so moi truong demo dat ma "TD" - coi "TD" la "LN" de doan tin dung van duoc nhan dien. */
    private Set<String> normalizeSegmentCodes(List<String> codes) {
        Set<String> result = new HashSet<>();
        for (String code : codes) {
            result.add("TD".equals(code) ? AuditKhnsPbAllocator.CREDIT : code);
        }
        return result;
    }

    private Set<Integer> monthsOf(AuditKhktThangRowResponse r) {
        Set<Integer> months = new TreeSet<>();
        boolean[] flags = {r.month1(), r.month2(), r.month3(), r.month4(), r.month5(), r.month6(),
                r.month7(), r.month8(), r.month9(), r.month10(), r.month11(), r.month12()};
        for (int i = 0; i < flags.length; i++) {
            if (flags[i]) {
                months.add(i + 1);
            }
        }
        return months;
    }

    /** Muc quy mo tu STT muc quy mo (KHKT_QM): cao nhat = Lon, cao hon thap nhat = Trung binh, con lai = Thap. */
    private ScaleLevel levelOf(Integer scale, List<AuditKhktScaleResponse> scales) {
        if (scale == null || scales.size() < 2) {
            return ScaleLevel.LOW;
        }
        int min = scales.stream().mapToInt(AuditKhktScaleResponse::sortOrder).min().orElse(scale);
        int max = scales.stream().mapToInt(AuditKhktScaleResponse::sortOrder).max().orElse(scale);
        if (scale >= max) {
            return ScaleLevel.LARGE;
        }
        return scale > min ? ScaleLevel.MEDIUM : ScaleLevel.LOW;
    }

    private Set<String> tokens(String semicolonSeparated) {
        Set<String> result = new HashSet<>();
        if (semicolonSeparated != null) {
            for (String part : semicolonSeparated.split(";")) {
                String token = normalize(part);
                if (!token.isEmpty()) {
                    result.add(token);
                }
            }
        }
        return result;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
