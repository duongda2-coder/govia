package com.govia.audit.tdkp.report;

import com.govia.audit.tdkp.common.TdkpSupport;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Sheet 7 - ZTC_TDKP_BC: xem trước/xuất 5 báo cáo TDKP (Excel) và lưu trữ báo cáo đã xuất. */
@RestController
@RequestMapping("/api/audit/tdkp/report")
public class AuditTdkpReportController {

    private final AuditTdkpReportService reportService;
    private final AuditTdkpReportArchiveService archiveService;

    public AuditTdkpReportController(AuditTdkpReportService reportService, AuditTdkpReportArchiveService archiveService) {
        this.reportService = reportService;
        this.archiveService = archiveService;
    }

    /** Xem trước dạng bảng (ALV): cùng dữ liệu với file Excel sẽ xuất. */
    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_BC.VIEW')")
    public ApiResponse<AuditTdkpReportDto.ReportData> preview(@Valid @RequestBody AuditTdkpReportDto.ReportRequest request) {
        return ApiResponse.ok(reportService.build(request));
    }

    @PostMapping("/export")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_BC.EXPORT')")
    public ResponseEntity<byte[]> export(@Valid @RequestBody AuditTdkpReportDto.ReportRequest request) {
        return TdkpSupport.xlsx("tdkp_" + request.type().name().toLowerCase() + ".xlsx", reportService.export(request));
    }

    @GetMapping("/archive")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_BC.VIEW')")
    public ApiResponse<List<AuditTdkpReportDto.ArchiveResponse>> listArchive() {
        return ApiResponse.ok(archiveService.list());
    }

    @PostMapping("/archive")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_BC.UPLOAD')")
    public ApiResponse<AuditTdkpReportDto.ArchiveResponse> createArchive(@Valid @RequestBody AuditTdkpReportDto.ArchiveRequest request) {
        return ApiResponse.ok(archiveService.create(request));
    }

    @PutMapping("/archive/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_BC.UPLOAD')")
    public ApiResponse<AuditTdkpReportDto.ArchiveResponse> updateArchive(@PathVariable UUID id, @Valid @RequestBody AuditTdkpReportDto.ArchiveRequest request) {
        return ApiResponse.ok(archiveService.update(id, request));
    }

    @DeleteMapping("/archive/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_BC.DELETE')")
    public ApiResponse<Void> deleteArchive(@PathVariable UUID id) {
        archiveService.delete(id);
        return ApiResponse.ok(null);
    }
}
