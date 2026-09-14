package com.govia.audit.khkt.report.controller;

import com.govia.audit.khkt.report.service.AuditKhktReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** "Đề xuất kế hoạch kiểm toán năm" - xuat Excel tu du lieu da xac nhan (sheet ZTC_KHKT_BCBP nguon
 * BP2, ZTC_KHKT_BCTH nguon TH2) - xem AuditKhktReportService. Dung lai quyen VIEW cua BP/TH, khong
 * can quyen rieng vi chi la 1 dang xuat cua du lieu da xem duoc. */
@RestController
@RequestMapping("/api/audit/plan/khkt-report")
public class AuditKhktReportController {

    private final AuditKhktReportService service;

    public AuditKhktReportController(AuditKhktReportService service) {
        this.service = service;
    }

    @GetMapping("/bp/export")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_BP.VIEW')")
    public ResponseEntity<byte[]> exportBp(@RequestParam Integer year) {
        return excelResponse(service.exportBpReport(year), "bao_cao_khkt_bp_" + year + ".xlsx");
    }

    @GetMapping("/th/export")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_TH.VIEW')")
    public ResponseEntity<byte[]> exportTh(@RequestParam Integer year) {
        return excelResponse(service.exportThReport(year), "bao_cao_khkt_th_" + year + ".xlsx");
    }

    private ResponseEntity<byte[]> excelResponse(byte[] content, String fileName) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }
}
