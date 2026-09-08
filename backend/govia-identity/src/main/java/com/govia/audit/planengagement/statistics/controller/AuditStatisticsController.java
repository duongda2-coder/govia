package com.govia.audit.planengagement.statistics.controller;

import com.govia.audit.planengagement.statistics.dto.EmployeeCodeOption;
import com.govia.audit.planengagement.statistics.dto.EmployeeStatRow;
import com.govia.audit.planengagement.statistics.dto.EngagementOption;
import com.govia.audit.planengagement.statistics.dto.EngagementSegmentStatRow;
import com.govia.audit.planengagement.statistics.dto.TtssDetailRow;
import com.govia.audit.planengagement.statistics.dto.UnitDetailStatRow;
import com.govia.audit.planengagement.statistics.dto.UnitStatRow;
import com.govia.audit.planengagement.statistics.dto.YearSegmentStatRow;
import com.govia.audit.planengagement.statistics.service.AuditStatisticsService;
import com.govia.core.web.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Man hinh "Thong ke" (tcode ztc_thongke, sheet "Thống kê" cua "Tao CKT (3).xlsx"). */
@RestController
@RequestMapping("/api/audit/statistics")
public class AuditStatisticsController {

    private final AuditStatisticsService service;

    public AuditStatisticsController(AuditStatisticsService service) {
        this.service = service;
    }

    @GetMapping("/lookups/years")
    @PreAuthorize("hasAuthority('PERM_AUDIT.STATISTICS.VIEW')")
    public ApiResponse<List<Integer>> lookupYears() {
        return ApiResponse.ok(service.listYearOptions());
    }

    @GetMapping("/lookups/engagements")
    @PreAuthorize("hasAuthority('PERM_AUDIT.STATISTICS.VIEW')")
    public ApiResponse<List<EngagementOption>> lookupEngagements() {
        return ApiResponse.ok(service.listEngagementOptions());
    }

    @GetMapping("/lookups/employees")
    @PreAuthorize("hasAuthority('PERM_AUDIT.STATISTICS.VIEW')")
    public ApiResponse<List<EmployeeCodeOption>> lookupEmployees() {
        return ApiResponse.ok(service.listEmployeeOptions());
    }

    @GetMapping("/by-year")
    @PreAuthorize("hasAuthority('PERM_AUDIT.STATISTICS.VIEW')")
    public ApiResponse<List<YearSegmentStatRow>> byYear(@RequestParam(required = false) Integer year) {
        return ApiResponse.ok(service.statsByYear(year));
    }

    @GetMapping("/by-year/ttss-detail")
    @PreAuthorize("hasAuthority('PERM_AUDIT.STATISTICS.VIEW')")
    public ApiResponse<List<TtssDetailRow>> ttssDetail(@RequestParam Integer year, @RequestParam UUID businessSegmentId) {
        return ApiResponse.ok(service.ttssDetailByYearSegment(year, businessSegmentId));
    }

    @GetMapping("/by-engagement")
    @PreAuthorize("hasAuthority('PERM_AUDIT.STATISTICS.VIEW')")
    public ApiResponse<List<EngagementSegmentStatRow>> byEngagement(@RequestParam(required = false) String engagementCode) {
        return ApiResponse.ok(service.statsByEngagement(engagementCode));
    }

    @GetMapping("/by-employee")
    @PreAuthorize("hasAuthority('PERM_AUDIT.STATISTICS.VIEW')")
    public ApiResponse<List<EmployeeStatRow>> byEmployee(@RequestParam(required = false) String employeeCode) {
        return ApiResponse.ok(service.statsByEmployee(employeeCode));
    }

    @GetMapping("/by-unit")
    @PreAuthorize("hasAuthority('PERM_AUDIT.STATISTICS.VIEW')")
    public ApiResponse<List<UnitStatRow>> byUnit() {
        return ApiResponse.ok(service.statsByUnit());
    }

    @GetMapping("/by-unit/{auditObjectUnitId}/detail")
    @PreAuthorize("hasAuthority('PERM_AUDIT.STATISTICS.VIEW')")
    public ApiResponse<List<UnitDetailStatRow>> unitDetail(@PathVariable UUID auditObjectUnitId) {
        return ApiResponse.ok(service.unitDetail(auditObjectUnitId));
    }

    @GetMapping("/by-year/export")
    @PreAuthorize("hasAuthority('PERM_AUDIT.STATISTICS.EXPORT')")
    public ResponseEntity<byte[]> exportByYear(@RequestParam(required = false) Integer year) {
        return excelResponse("thong_ke_theo_nam.xlsx", service.exportByYear(year));
    }

    @GetMapping("/by-engagement/export")
    @PreAuthorize("hasAuthority('PERM_AUDIT.STATISTICS.EXPORT')")
    public ResponseEntity<byte[]> exportByEngagement(@RequestParam(required = false) String engagementCode) {
        return excelResponse("thong_ke_theo_ckt.xlsx", service.exportByEngagement(engagementCode));
    }

    @GetMapping("/by-employee/export")
    @PreAuthorize("hasAuthority('PERM_AUDIT.STATISTICS.EXPORT')")
    public ResponseEntity<byte[]> exportByEmployee(@RequestParam(required = false) String employeeCode) {
        return excelResponse("thong_ke_theo_thanh_vien.xlsx", service.exportByEmployee(employeeCode));
    }

    @GetMapping("/by-unit/export")
    @PreAuthorize("hasAuthority('PERM_AUDIT.STATISTICS.EXPORT')")
    public ResponseEntity<byte[]> exportByUnit() {
        return excelResponse("thong_ke_theo_don_vi.xlsx", service.exportByUnit());
    }

    private ResponseEntity<byte[]> excelResponse(String filename, byte[] content) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }
}
