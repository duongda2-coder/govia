package com.govia.audit.tdkp.assignment;

import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.tdkp.common.TdkpMasterData;
import com.govia.audit.tdkp.common.TdkpTarget;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Employee;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Màn hình "Phân công Theo dõi khắc phục" (ZTC_TDKP_PC): CRUD + extract Excel cho 2 phần HĐTV/TGĐ và Chi nhánh. */
@Service
public class AuditTdkpAssignmentService {

    private final AuditTdkpAssignmentRepository repository;
    private final TdkpMasterData masterData;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;

    public AuditTdkpAssignmentService(AuditTdkpAssignmentRepository repository, TdkpMasterData masterData,
                                      AuditLogService auditLogService, ExcelExportService excelExportService) {
        this.repository = repository;
        this.masterData = masterData;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
    }

    @Transactional(readOnly = true)
    public List<AuditTdkpAssignmentDto.Response> list(TdkpAssignmentScope scope) {
        List<AuditTdkpAssignment> items = repository.findByTenantIdAndScopeOrderByCreatedAtAsc(TenantContext.getTenantId(), scope);
        Lookup lookup = lookup(items);
        return items.stream().map(item -> toResponse(item, lookup)).toList();
    }

    @Transactional
    public AuditTdkpAssignmentDto.Response create(AuditTdkpAssignmentDto.Request request) {
        AuditTdkpAssignment item = new AuditTdkpAssignment();
        item.setTenantId(TenantContext.getTenantId());
        apply(item, request);
        item = repository.save(item);
        auditLogService.record("AuditTdkpAssignment", item.getId(), AuditAction.CREATE, "Tao phan cong theo doi khac phuc (" + item.getScope() + ")");
        return toResponse(item, lookup(List.of(item)));
    }

    @Transactional
    public AuditTdkpAssignmentDto.Response update(UUID id, AuditTdkpAssignmentDto.Request request) {
        AuditTdkpAssignment item = getOwnedOrThrow(id);
        apply(item, request);
        item = repository.save(item);
        auditLogService.record("AuditTdkpAssignment", item.getId(), AuditAction.UPDATE, "Cap nhat phan cong theo doi khac phuc (" + item.getScope() + ")");
        return toResponse(item, lookup(List.of(item)));
    }

    @Transactional
    public void delete(UUID id) {
        AuditTdkpAssignment item = getOwnedOrThrow(id);
        repository.delete(item);
        auditLogService.record("AuditTdkpAssignment", id, AuditAction.DELETE, "Xoa phan cong theo doi khac phuc (" + item.getScope() + ")");
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(TdkpAssignmentScope scope) {
        boolean ceo = scope == TdkpAssignmentScope.CEO;
        List<ExportColumn> columns = ceo
                ? List.of(new ExportColumn("recommendationTypeName", "Phân loại kiến nghị"), new ExportColumn("businessSegmentCode", "Mã mảng nghiệp vụ"),
                new ExportColumn("targetObjectLabel", "Đối tượng được kiến nghị"), new ExportColumn("auditObjectUnitCode", "Mã đối tượng kiểm toán"),
                new ExportColumn("geographicAreaName", "Khu vực địa lý"), new ExportColumn("employeeName", "Cán bộ phụ trách"),
                new ExportColumn("username", "User phụ trách"), new ExportColumn("departmentName", "Phòng nghiệp vụ"),
                new ExportColumn("startDate", "Ngày bắt đầu"), new ExportColumn("endDate", "Ngày kết thúc"))
                : List.of(new ExportColumn("auditObjectUnitName", "Đối tượng được kiến nghị"), new ExportColumn("auditObjectUnitCode", "Mã đối tượng được kiến nghị"),
                new ExportColumn("recommendationTypeName", "Phân loại kiến nghị"), new ExportColumn("businessSegmentCode", "Mã mảng nghiệp vụ"),
                new ExportColumn("geographicAreaName", "Khu vực địa lý"), new ExportColumn("employeeName", "Cán bộ phụ trách"),
                new ExportColumn("username", "User phụ trách"), new ExportColumn("departmentName", "Phòng nghiệp vụ"),
                new ExportColumn("startDate", "Ngày bắt đầu"), new ExportColumn("endDate", "Ngày kết thúc"));
        List<Map<String, Object>> rows = list(scope).stream().map(r -> {
            Map<String, Object> row = new HashMap<>();
            row.put("recommendationTypeName", r.recommendationTypeName());
            row.put("businessSegmentCode", r.businessSegmentCode());
            row.put("targetObjectLabel", r.targetObjectLabel());
            row.put("auditObjectUnitCode", r.auditObjectUnitCode());
            row.put("auditObjectUnitName", r.auditObjectUnitName());
            row.put("geographicAreaName", r.geographicAreaName());
            row.put("employeeName", r.employeeCode() == null ? r.employeeName() : r.employeeCode() + " - " + r.employeeName());
            row.put("username", r.username());
            row.put("departmentName", r.departmentName());
            row.put("startDate", r.startDate());
            row.put("endDate", r.endDate());
            return row;
        }).toList();
        return excelExportService.export(ceo ? "TDKP_PC_HDTV_TGD" : "TDKP_PC_CHI_NHANH", columns, rows);
    }

    private void apply(AuditTdkpAssignment item, AuditTdkpAssignmentDto.Request request) {
        if (request.startDate() != null && request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("TDKP_END_BEFORE_START", "Ngay ket thuc khong duoc truoc Ngay bat dau");
        }
        if (request.scope() == TdkpAssignmentScope.CEO && request.targetObject() == null) {
            throw new BusinessException("TDKP_TARGET_REQUIRED", "Phai chon Doi tuong duoc kien nghi (HDTV/TGD)");
        }
        if (request.scope() == TdkpAssignmentScope.BRANCH && request.auditObjectUnitId() == null) {
            throw new BusinessException("TDKP_UNIT_REQUIRED", "Phai chon Doi tuong duoc kien nghi (don vi)");
        }
        item.setScope(request.scope());
        item.setRecommendationTypeId(masterData.requireItem(AuditMasterDataCategory.RECOMMENDATION_TYPE, request.recommendationTypeId(), "Phan loai kien nghi"));
        item.setBusinessSegmentId(masterData.requireItem(AuditMasterDataCategory.BUSINESS_SEGMENT, request.businessSegmentId(), "Ma mang nghiep vu"));
        item.setTargetObject(request.scope() == TdkpAssignmentScope.CEO ? request.targetObject() : null);
        item.setAuditObjectUnitId(masterData.requireUnit(request.auditObjectUnitId(), "Doi tuong kiem toan"));
        item.setGeographicAreaId(masterData.requireItem(AuditMasterDataCategory.GEOGRAPHIC_AREA, request.geographicAreaId(), "Khu vuc dia ly"));
        item.setEmployeeId(masterData.requireEmployee(request.employeeId(), "Can bo phu trach"));
        item.setStartDate(request.startDate());
        item.setEndDate(request.endDate());
    }

    private AuditTdkpAssignment getOwnedOrThrow(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findById(id).filter(i -> i.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("TDKP_ASSIGNMENT_NOT_FOUND", "Khong tim thay phan cong theo doi khac phuc", HttpStatus.NOT_FOUND));
    }

    private record Lookup(Map<UUID, AuditMasterDataItem> types, Map<UUID, AuditMasterDataItem> segments, Map<UUID, AuditMasterDataItem> areas,
                          Map<UUID, AuditMasterDataItem> departments, Map<UUID, AuditObjectUnit> units, Map<UUID, Employee> employees,
                          Map<UUID, String> usernames) {
    }

    private Lookup lookup(List<AuditTdkpAssignment> items) {
        Map<UUID, Employee> employees = masterData.employees();
        return new Lookup(masterData.items(AuditMasterDataCategory.RECOMMENDATION_TYPE), masterData.items(AuditMasterDataCategory.BUSINESS_SEGMENT),
                masterData.items(AuditMasterDataCategory.GEOGRAPHIC_AREA), masterData.items(AuditMasterDataCategory.DEPARTMENT), masterData.units(),
                employees, masterData.usernames(items.stream().map(AuditTdkpAssignment::getEmployeeId).distinct().toList()));
    }

    private AuditTdkpAssignmentDto.Response toResponse(AuditTdkpAssignment item, Lookup lookup) {
        Employee employee = lookup.employees().get(item.getEmployeeId());
        AuditObjectUnit unit = item.getAuditObjectUnitId() == null ? null : lookup.units().get(item.getAuditObjectUnitId());
        return new AuditTdkpAssignmentDto.Response(item.getId(), item.getScope(), item.getRecommendationTypeId(),
                TdkpMasterData.nameOf(lookup.types(), item.getRecommendationTypeId()), item.getBusinessSegmentId(),
                TdkpMasterData.codeOf(lookup.segments(), item.getBusinessSegmentId()), TdkpMasterData.nameOf(lookup.segments(), item.getBusinessSegmentId()),
                item.getTargetObject(), TdkpTarget.labelOf(item.getTargetObject()), item.getAuditObjectUnitId(),
                unit == null ? null : unit.getCode(), unit == null ? null : unit.getName(), item.getGeographicAreaId(),
                TdkpMasterData.nameOf(lookup.areas(), item.getGeographicAreaId()), item.getEmployeeId(),
                employee == null ? null : employee.getEmployeeCode(), employee == null ? null : employee.getFullName(),
                lookup.usernames().get(item.getEmployeeId()),
                employee == null ? null : TdkpMasterData.nameOf(lookup.departments(), employee.getDepartmentId()),
                item.getStartDate(), item.getEndDate());
    }
}
