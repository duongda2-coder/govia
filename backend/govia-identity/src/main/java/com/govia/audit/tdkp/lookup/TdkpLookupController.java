package com.govia.audit.tdkp.lookup;

import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.tdkp.common.TdkpMasterData;
import com.govia.core.web.ApiResponse;
import com.govia.identity.entity.Employee;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Các danh sách chọn (list) dùng chung cho nhóm màn hình Theo dõi khắc phục - 1 lần gọi cho mọi màn hình. */
@RestController
@RequestMapping("/api/audit/tdkp/lookups")
public class TdkpLookupController {

    public record Option(UUID id, String code, String name) {
    }

    public record UnitOption(UUID id, String code, String name, String unitType) {
    }

    public record EmployeeOption(UUID id, String code, String name, String username, String departmentName) {
    }

    public record Lookups(List<Option> recommendationTypes, List<Option> businessSegments, List<Option> geographicAreas,
                          List<UnitOption> units, List<EmployeeOption> employees) {
    }

    private final TdkpMasterData masterData;

    public TdkpLookupController(TdkpMasterData masterData) {
        this.masterData = masterData;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('PERM_AUDIT.TDKP_PC.VIEW','PERM_AUDIT.TDKP_CEO_ALL.VIEW','PERM_AUDIT.TDKP_CEO_KH.VIEW','PERM_AUDIT.TDKP_CN.VIEW',"
            + "'PERM_AUDIT.TDKP_NQ.VIEW','PERM_AUDIT.TDKP_KTNB.VIEW','PERM_AUDIT.TDKP_BC.VIEW')")
    public ApiResponse<Lookups> lookups() {
        Map<UUID, AuditMasterDataItem> departments = masterData.items(AuditMasterDataCategory.DEPARTMENT);
        List<Employee> employees = List.copyOf(masterData.employees().values());
        Map<UUID, String> usernames = masterData.usernames(employees.stream().map(Employee::getId).toList());
        return ApiResponse.ok(new Lookups(
                options(AuditMasterDataCategory.RECOMMENDATION_TYPE), options(AuditMasterDataCategory.BUSINESS_SEGMENT),
                options(AuditMasterDataCategory.GEOGRAPHIC_AREA),
                masterData.units().values().stream().map(u -> new UnitOption(u.getId(), u.getCode(), u.getName(), u.getUnitType())).toList(),
                employees.stream().map(e -> new EmployeeOption(e.getId(), e.getEmployeeCode(), e.getFullName(), usernames.get(e.getId()),
                        TdkpMasterData.nameOf(departments, e.getDepartmentId()))).toList()));
    }

    private List<Option> options(AuditMasterDataCategory category) {
        return masterData.items(category).values().stream().map(i -> new Option(i.getId(), i.getCode(), i.getName())).toList();
    }
}
