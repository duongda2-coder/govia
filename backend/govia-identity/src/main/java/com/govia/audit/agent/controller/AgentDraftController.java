package com.govia.audit.agent.controller;

import com.govia.audit.agent.dto.DgclScoreDraftRequest;
import com.govia.audit.agent.dto.DgclScoreDraftResponse;
import com.govia.audit.agent.dto.ImportCheckResponse;
import com.govia.audit.agent.dto.RecommendationDraftRequest;
import com.govia.audit.agent.dto.RecommendationDraftResponse;
import com.govia.audit.agent.dto.ReminderDraftRequest;
import com.govia.audit.agent.dto.ReminderDraftResponse;
import com.govia.audit.agent.dto.RewriteRequest;
import com.govia.audit.agent.dto.RewriteResponse;
import com.govia.audit.agent.service.AgentDraftService;
import com.govia.audit.agent.service.AgentImportCheckService;
import com.govia.audit.agent.service.AgentQualityDraftService;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Soan nhap bang AI (G2, muc M2) - chi TRA VE ban nhap, khong luu gi. Soan kien nghi can them quyen xem
 * TTSS (kiem tra lai ben trong qua AuditTtssController, gom ca phan quyen theo dong).
 */
@RestController
@RequestMapping("/api/audit/agent/drafts")
public class AgentDraftController {

    private final AgentDraftService draftService;
    private final AgentQualityDraftService qualityDraftService;
    private final AgentImportCheckService importCheckService;

    public AgentDraftController(AgentDraftService draftService, AgentQualityDraftService qualityDraftService,
                                AgentImportCheckService importCheckService) {
        this.draftService = draftService;
        this.qualityDraftService = qualityDraftService;
        this.importCheckService = importCheckService;
    }

    /** G4 - "Gợi ý chấm ĐGCL" (A6): chi tra goi y, khong luu phieu; quyen vao CKT kiem tra lai trong AuditDgclService. */
    @PostMapping("/dgcl-score")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.VIEW') and hasAuthority('PERM_AUDIT.DGCL.VIEW')")
    public ApiResponse<DgclScoreDraftResponse> draftDgclScore(@Valid @RequestBody DgclScoreDraftRequest request,
                                                              @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(qualityDraftService.suggest(request, principal));
    }

    /** G4 - "Kiểm tra file trước khi import" (A7): khong import, khong luu file; quyen VIEW + EXPORT cua danh muc
     * duoc kiem tra lai qua controller danh muc. */
    @PostMapping("/import-check")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.VIEW')")
    public ApiResponse<ImportCheckResponse> importCheck(@RequestParam("catalog") String catalog, @RequestParam("file") MultipartFile file,
                                                        @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(importCheckService.check(catalog, file, principal));
    }

    @PostMapping("/recommendations")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.VIEW') and hasAuthority('PERM_AUDIT.TTSS.VIEW')")
    public ApiResponse<RecommendationDraftResponse> draftRecommendations(@Valid @RequestBody RecommendationDraftRequest request,
                                                                         @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(draftService.draftRecommendations(request, principal));
    }

    /** Thu don doc TDKP - quyen xem danh sach TDKP tuong ung duoc kiem tra lai ben trong (controller TDKP). */
    @PostMapping("/reminder")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.VIEW')")
    public ApiResponse<ReminderDraftResponse> draftReminder(@Valid @RequestBody ReminderDraftRequest request,
                                                            @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(draftService.draftReminder(request, principal));
    }

    @PostMapping("/rewrite")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.VIEW')")
    public ApiResponse<RewriteResponse> rewrite(@Valid @RequestBody RewriteRequest request,
                                                @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(draftService.rewrite(request, principal));
    }
}
