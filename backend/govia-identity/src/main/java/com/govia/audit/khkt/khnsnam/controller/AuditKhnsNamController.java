package com.govia.audit.khkt.khnsnam.controller;

import com.govia.audit.khkt.khnsnam.dto.AuditKhnsNamRowResponse;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsNamUpdateRequest;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsPbAllocationResult;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsPbRowResponse;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsTransferCandidateResponse;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsTransferRequest;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsTransferResultItem;
import com.govia.audit.khkt.khnsnam.service.AuditKhktTransferService;
import com.govia.audit.khkt.khnsnam.service.AuditKhnsNamService;
import com.govia.audit.khkt.khnsnam.service.AuditKhnsPbService;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
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

import java.util.List;
import java.util.UUID;

/** "Dự kiến nhân sự thực hiện kiểm toán năm, đợt" (sheet ZTC_KHNS_NAM) - xem AuditKhnsNamService. */
@RestController
@RequestMapping("/api/audit/plan/khns-nam")
public class AuditKhnsNamController {

    private final AuditKhnsNamService service;
    private final AuditKhktTransferService transferService;
    private final AuditKhnsPbService pbService;

    public AuditKhnsNamController(AuditKhnsNamService service, AuditKhktTransferService transferService, AuditKhnsPbService pbService) {
        this.service = service;
        this.transferService = transferService;
        this.pbService = pbService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHNS_NAM.VIEW')")
    public ApiResponse<List<AuditKhnsNamRowResponse>> list(@RequestParam Integer year,
                                                            @RequestParam(defaultValue = "false") boolean allocatedOnly) {
        return ApiResponse.ok(service.list(year, allocatedOnly));
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

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHNS_NAM.VIEW')")
    public ResponseEntity<byte[]> exportMonthlyReport(@RequestParam Integer year, @RequestParam Integer month) {
        byte[] content = service.exportMonthlyReport(year, month);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bao_cao_khns_thang_" + month + "_" + year + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
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
