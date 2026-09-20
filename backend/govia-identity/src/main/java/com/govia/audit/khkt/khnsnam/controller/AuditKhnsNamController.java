package com.govia.audit.khkt.khnsnam.controller;

import com.govia.audit.khkt.khnsnam.dto.AuditKhnsNamBatchReportRequest;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsNamInfoRequest;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsNamRowResponse;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsNamUpdateRequest;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsPbAllocationResult;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsPbRowResponse;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsTransferCandidateResponse;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsTransferRequest;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsTransferResultItem;
import com.govia.audit.khkt.khnsnam.service.AuditKhktTransferService;
import com.govia.audit.khkt.khnsnam.service.AuditKhnsNamDecisionService;
import com.govia.audit.khkt.khnsnam.service.AuditKhnsNamService;
import com.govia.audit.khkt.khnsnam.service.AuditKhnsPbService;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/** "Dự kiến nhân sự thực hiện kiểm toán năm, đợt" (sheet ZTC_KHNS_NAM) - xem AuditKhnsNamService. */
@RestController
@RequestMapping("/api/audit/plan/khns-nam")
public class AuditKhnsNamController {

    private final AuditKhnsNamService service;
    private final AuditKhktTransferService transferService;
    private final AuditKhnsPbService pbService;
    private final AuditKhnsNamDecisionService decisionService;

    public AuditKhnsNamController(AuditKhnsNamService service, AuditKhktTransferService transferService, AuditKhnsPbService pbService,
                                  AuditKhnsNamDecisionService decisionService) {
        this.service = service;
        this.transferService = transferService;
        this.pbService = pbService;
        this.decisionService = decisionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHNS_NAM.VIEW')")
    public ApiResponse<List<AuditKhnsNamRowResponse>> list(@RequestParam Integer year,
                                                            @RequestParam(defaultValue = "false") boolean allocatedOnly,
                                                            @RequestParam(defaultValue = "false") boolean listedOnly) {
        return ApiResponse.ok(service.list(year, allocatedOnly, listedOnly));
    }

    /** Nut "Cap nhat danh sach can bo" o KHNS_NAM - lay danh sach can bo da phan bo o KHNS_PB. Tra ve so can bo trong danh sach. */
    @PostMapping("/sync-list")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHNS_NAM.EDIT')")
    public ApiResponse<Integer> syncList(@RequestParam Integer year) {
        return ApiResponse.ok(service.syncListFromAllocation(year));
    }

    /** Sua cac truong nhap tay cua man hinh KHNS_NAM (khong dong vao phan bo thang/chuc vu). */
    @PutMapping("/{employeeId}/info")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHNS_NAM.EDIT')")
    public ApiResponse<AuditKhnsNamRowResponse> updateInfo(@PathVariable UUID employeeId, @RequestParam Integer year,
                                                            @Valid @RequestBody AuditKhnsNamInfoRequest request) {
        return ApiResponse.ok(service.updateInfo(employeeId, year, request));
    }

    @PutMapping("/{employeeId}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHNS_NAM.EDIT')")
    public ApiResponse<AuditKhnsNamRowResponse> update(@PathVariable UUID employeeId, @RequestParam Integer year,
                                                        @Valid @RequestBody AuditKhnsNamUpdateRequest request) {
        return ApiResponse.ok(service.update(employeeId, year, request));
    }

    /** "Ghi chú" cua man hinh KHNS_PB (bao cao tong hop tu du lieu nay) - CHI sua truong note. */
    @PutMapping("/{employeeId}/note")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHNS_NAM.EDIT')")
    public ApiResponse<AuditKhnsNamRowResponse> updateNote(@PathVariable UUID employeeId, @RequestParam Integer year,
                                                            @RequestBody NoteRequest request) {
        return ApiResponse.ok(service.updateNote(employeeId, year, request.note()));
    }

    public record NoteRequest(String note) {
    }

    /** Cac dong man hinh KHNS_PB (sheet ZTC_KHNS_PB): moi can bo duoc phan bo vao moi don vi 1 dong - xem AuditKhnsPbService. */
    @GetMapping("/allocation")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHNS_NAM.VIEW')")
    public ApiResponse<List<AuditKhnsPbRowResponse>> allocationRows(@RequestParam Integer year) {
        return ApiResponse.ok(pbService.listRows(year));
    }

    /** Nut "Phan bo nhan su" o KHNS_PB - tu dong phan bo can bo cho ca nam, xem AuditKhnsPbService. */
    @PostMapping("/allocate")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHNS_NAM.EDIT')")
    public ApiResponse<AuditKhnsPbAllocationResult> allocate(@RequestParam Integer year) {
        return ApiResponse.ok(pbService.allocate(year));
    }

    /** "Xuat bao cao theo dot" (file mau ZTC_BC_DOT) - POST vi NSD nhap them so/ngay quyet dinh va thoi gian kiem toan tung don vi. */
    @PostMapping("/export")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHNS_NAM.VIEW')")
    public ResponseEntity<byte[]> exportMonthlyReport(@RequestParam Integer year, @RequestParam Integer month,
                                                       @RequestBody(required = false) AuditKhnsNamBatchReportRequest request) {
        byte[] content = service.exportMonthlyReport(year, month, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bao_cao_khns_thang_" + month + "_" + year + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    /** "Xuat QD thanh lap doan" / "Xuat QD kiem ke" (file Word mau cua BKS) cho 1 chi nhanh di kiem toan trong thang - xem AuditKhnsNamDecisionService. */
    @GetMapping("/export-decision")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHNS_NAM.VIEW')")
    public ResponseEntity<byte[]> exportDecision(@RequestParam Integer year, @RequestParam Integer month, @RequestParam String auditObjectCode,
                                                  @RequestParam AuditKhnsNamDecisionService.Type type) {
        AuditKhnsNamDecisionService.DecisionFile file = decisionService.export(type, year, month, auditObjectCode);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(file.content());
    }

    /** "Chuyển thông tin KHTH" (sheet ChuyenthongtinKHTH) - xem AuditKhktTransferService. */
    @GetMapping("/transfer-candidates")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHTH_TRANSFER.VIEW')")
    public ApiResponse<List<AuditKhnsTransferCandidateResponse>> transferCandidates(@RequestParam Integer year) {
        return ApiResponse.ok(transferService.listCandidates(year));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHTH_TRANSFER.EXECUTE')")
    public ApiResponse<List<AuditKhnsTransferResultItem>> transfer(@RequestParam Integer year, @RequestBody AuditKhnsTransferRequest request) {
        return ApiResponse.ok(transferService.transfer(year, request.auditObjectCodes()));
    }
}
