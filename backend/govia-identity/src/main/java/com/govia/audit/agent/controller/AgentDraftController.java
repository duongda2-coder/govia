package com.govia.audit.agent.controller;

import com.govia.audit.agent.dto.RecommendationDraftRequest;
import com.govia.audit.agent.dto.RecommendationDraftResponse;
import com.govia.audit.agent.dto.RewriteRequest;
import com.govia.audit.agent.dto.RewriteResponse;
import com.govia.audit.agent.service.AgentDraftService;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Soan nhap bang AI (G2, muc M2) - chi TRA VE ban nhap, khong luu gi. Soan kien nghi can them quyen xem
 * TTSS (kiem tra lai ben trong qua AuditTtssController, gom ca phan quyen theo dong).
 */
@RestController
@RequestMapping("/api/audit/agent/drafts")
public class AgentDraftController {

    private final AgentDraftService draftService;

    public AgentDraftController(AgentDraftService draftService) {
        this.draftService = draftService;
    }

    @PostMapping("/recommendations")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.VIEW') and hasAuthority('PERM_AUDIT.TTSS.VIEW')")
    public ApiResponse<RecommendationDraftResponse> draftRecommendations(@Valid @RequestBody RecommendationDraftRequest request,
                                                                         @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(draftService.draftRecommendations(request, principal));
    }

    @PostMapping("/rewrite")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.VIEW')")
    public ApiResponse<RewriteResponse> rewrite(@Valid @RequestBody RewriteRequest request,
                                                @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(draftService.rewrite(request, principal));
    }
}
