package com.govia.audit.khkt.khnsnam.service;

import com.govia.audit.khkt.khnsnam.dto.AuditKhnsPbRowResponse;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.Gender;
import com.govia.identity.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Xuat 2 quyet dinh Word tu man hinh KHNS_NAM cho 1 chi nhanh (doi tuong kiem toan) di kiem toan trong 1 thang (dot):
 * "QD thanh lap doan" va "QD kiem ke" - danh sach can bo la doan kiem toan cua chi nhanh do o KHNS_PB. Xem AuditKhnsNamDecisionWriter. */
@Service
public class AuditKhnsNamDecisionService {

    public enum Type {
        TEAM("QD thanh lap doan", "Xuat QD thanh lap doan"),
        INVENTORY("QD kiem ke", "Xuat QD kiem ke");

        private final String fileBaseName;
        private final String auditDescription;

        Type(String fileBaseName, String auditDescription) {
            this.fileBaseName = fileBaseName;
            this.auditDescription = auditDescription;
        }
    }

    public record DecisionFile(byte[] content, String fileName) {
    }

    private final AuditKhnsPbService pbService;
    private final EmployeeRepository employeeRepository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditLogService auditLogService;

    public AuditKhnsNamDecisionService(AuditKhnsPbService pbService, EmployeeRepository employeeRepository,
                                       AuditMasterDataItemRepository masterDataItemRepository, AuditLogService auditLogService) {
        this.pbService = pbService;
        this.employeeRepository = employeeRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.auditLogService = auditLogService;
    }

    /** Doan kiem toan = cac can bo KHNS_PB phan bo vao don vi auditObjectCode trong thang month, Truong doan truoc, roi Truong nhom, Thanh vien. */
    @Transactional(readOnly = true)
    public DecisionFile export(Type type, Integer year, Integer month, String auditObjectCode) {
        if (month == null || month < 1 || month > 12) {
            throw new BusinessException("INVALID_MONTH", "Thang khong hop le: " + month);
        }
        UUID tenantId = TenantContext.getTenantId();
        List<AuditKhnsPbRowResponse> rows = pbService.listRows(year).stream()
                .filter(r -> r.months().contains(month) && r.auditObjectCode().equals(auditObjectCode))
                .sorted(Comparator.comparingInt((AuditKhnsPbRowResponse r) -> AuditKhnsNamService.roleRank(r.positions(), r.roleInTeam()))
                        .thenComparing(AuditKhnsPbRowResponse::employeeName))
                .toList();
        if (rows.isEmpty()) {
            throw new BusinessException("KHNS_DECISION_NO_TEAM",
                    "Don vi " + auditObjectCode + " chua co can bo nao duoc phan bo trong thang " + month + "/" + year);
        }

        Map<UUID, Employee> employees = employeeRepository.findByTenantIdOrderByFullNameAsc(tenantId).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));
        Map<UUID, AuditMasterDataItem> positions = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.POSITION);
        Map<UUID, AuditMasterDataItem> segments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT);

        List<AuditKhnsNamDecisionWriter.Member> members = rows.stream()
                .filter(r -> employees.containsKey(UUID.fromString(r.employeeId())))
                .map(r -> {
                    Employee employee = employees.get(UUID.fromString(r.employeeId()));
                    AuditMasterDataItem position = employee.getPositionId() == null ? null : positions.get(employee.getPositionId());
                    AuditMasterDataItem segment = employee.getBusinessSegmentId() == null ? null : segments.get(employee.getBusinessSegmentId());
                    return new AuditKhnsNamDecisionWriter.Member(salutation(employee.getGender()), employee.getFullName(),
                            position == null ? null : position.getName(),
                            AuditKhnsNamDecisionWriter.teamRole(r.positions(), r.roleInTeam(), segment == null ? null : segment.getCode()),
                            employee.getIdIssueDate(), employee.getIdIssuePlace());
                }).toList();

        String branchName = rows.get(0).auditObjectName();
        byte[] content = type == Type.TEAM
                ? AuditKhnsNamDecisionWriter.writeTeamDecision(branchName, members)
                : AuditKhnsNamDecisionWriter.writeInventoryDecision(branchName, members);
        auditLogService.record("AuditKhnsNam", null, AuditAction.EXPORT,
                type.auditDescription + " nam " + year + " thang " + month + ": " + branchName + " (" + members.size() + " can bo)");
        return new DecisionFile(content, AuditKhnsNamDecisionWriter.fileName(type.fileBaseName, branchName, month, year));
    }

    private static String salutation(Gender gender) {
        if (gender == Gender.MALE) {
            return "Ông";
        }
        return gender == Gender.FEMALE ? "Bà" : "Ông/Bà";
    }

    private Map<UUID, AuditMasterDataItem> masterDataItemsByCategory(UUID tenantId, AuditMasterDataCategory category) {
        return masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, category).stream()
                .collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
    }
}
