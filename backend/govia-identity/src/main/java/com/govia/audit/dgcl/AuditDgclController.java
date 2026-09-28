package com.govia.audit.dgcl;

import com.govia.audit.dgcl.AuditDgclDto.Capability;
import com.govia.audit.dgcl.AuditDgclDto.SaveRequest;
import com.govia.audit.dgcl.AuditDgclDto.Sheet;
import com.govia.audit.dgcl.AuditDgclDto.SubjectRow;
import com.govia.audit.planengagement.dto.AuditEngagementResponse;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Phan he "Đánh giá chất lượng" (DGCL_CN.xlsx) - xem {@link AuditDgclService}. Moi endpoint chi can AUDIT.DGCL.VIEW,
 * quyen danh gia/kiem soat kiem tra trong service theo 2 cot KNDN. */
@RestController
@RequestMapping("/api/audit/dgcl")
public class AuditDgclController {

    private final AuditDgclService service;

    public AuditDgclController(AuditDgclService service) {
        this.service = service;
    }

    @GetMapping("/engagements")
    @PreAuthorize("hasAuthority('PERM_AUDIT.DGCL.VIEW')")
    public ApiResponse<List<AuditEngagementResponse>> listEngagements() {
        return ApiResponse.ok(service.listEngagements());
    }

    @GetMapping("/capability")
    @PreAuthorize("hasAuthority('PERM_AUDIT.DGCL.VIEW')")
    public ApiResponse<Capability> capability(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.capability(principal));
    }

    @GetMapping("/engagements/{engagementId}/subjects")
    @PreAuthorize("hasAuthority('PERM_AUDIT.DGCL.VIEW')")
    public ApiResponse<List<SubjectRow>> listSubjects(@PathVariable UUID engagementId) {
        return ApiResponse.ok(service.listSubjects(engagementId));
    }

    @GetMapping("/engagements/{engagementId}/subjects/{subjectKey}/{appendix}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.DGCL.VIEW')")
    public ApiResponse<Sheet> getSheet(@PathVariable UUID engagementId, @PathVariable String subjectKey, @PathVariable DgclAppendix appendix,
                                       @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.getSheet(engagementId, subjectKey, appendix, principal));
    }

    @PutMapping("/engagements/{engagementId}/subjects/{subjectKey}/{appendix}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.DGCL.VIEW')")
    public ApiResponse<Sheet> saveSheet(@PathVariable UUID engagementId, @PathVariable String subjectKey, @PathVariable DgclAppendix appendix,
                                        @Valid @RequestBody SaveRequest request, @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.saveSheet(engagementId, subjectKey, appendix, request, principal));
    }

    @PostMapping("/engagements/{engagementId}/subjects/{subjectKey}/{appendix}/confirm")
    @PreAuthorize("hasAuthority('PERM_AUDIT.DGCL.VIEW')")
    public ApiResponse<Sheet> confirm(@PathVariable UUID engagementId, @PathVariable String subjectKey, @PathVariable DgclAppendix appendix,
                                      @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.confirm(engagementId, subjectKey, appendix, principal));
    }

    @PostMapping("/engagements/{engagementId}/subjects/{subjectKey}/{appendix}/unconfirm")
    @PreAuthorize("hasAuthority('PERM_AUDIT.DGCL.VIEW')")
    public ApiResponse<Sheet> unconfirm(@PathVariable UUID engagementId, @PathVariable String subjectKey, @PathVariable DgclAppendix appendix,
                                        @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.unconfirm(engagementId, subjectKey, appendix, principal));
    }

    @PostMapping("/engagements/{engagementId}/subjects/{subjectKey}/{appendix}/control")
    @PreAuthorize("hasAuthority('PERM_AUDIT.DGCL.VIEW')")
    public ApiResponse<Sheet> control(@PathVariable UUID engagementId, @PathVariable String subjectKey, @PathVariable DgclAppendix appendix,
                                      @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.control(engagementId, subjectKey, appendix, principal));
    }

    @PostMapping("/engagements/{engagementId}/subjects/{subjectKey}/{appendix}/uncontrol")
    @PreAuthorize("hasAuthority('PERM_AUDIT.DGCL.VIEW')")
    public ApiResponse<Sheet> uncontrol(@PathVariable UUID engagementId, @PathVariable String subjectKey, @PathVariable DgclAppendix appendix,
                                        @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.uncontrol(engagementId, subjectKey, appendix, principal));
    }

    @GetMapping("/engagements/{engagementId}/pl04b1")
    @PreAuthorize("hasAuthority('PERM_AUDIT.DGCL.EXPORT')")
    public ResponseEntity<byte[]> exportPl04b1(@PathVariable UUID engagementId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"pl04b1_dgcl.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.exportPl04b1(engagementId));
    }
}
