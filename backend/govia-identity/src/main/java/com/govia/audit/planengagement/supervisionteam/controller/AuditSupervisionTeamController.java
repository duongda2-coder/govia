package com.govia.audit.planengagement.supervisionteam.controller;

import com.govia.audit.planengagement.monitoring.dto.AuditEngagementMonitoringResponse;
import com.govia.audit.planengagement.supervisionteam.dto.AuditSupervisionCandidateResponse;
import com.govia.audit.planengagement.supervisionteam.dto.AuditSupervisionEvaluationResponse;
import com.govia.audit.planengagement.supervisionteam.dto.AuditSupervisionTeamMemberResponse;
import com.govia.audit.planengagement.supervisionteam.dto.SaveSupervisionEvaluationRequest;
import com.govia.audit.planengagement.supervisionteam.dto.SaveSupervisionTeamRequest;
import com.govia.audit.planengagement.supervisionteam.service.AuditSupervisionTeamService;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Sheet "To giam sat" cua Tao CKT (4).xlsx: chon to giam sat, to giam sat tu danh gia, xem ket
 * qua danh gia, va danh sach CKT ma nguoi dang dang nhap la thanh vien to giam sat. */
@RestController
public class AuditSupervisionTeamController {

    private final AuditSupervisionTeamService service;

    public AuditSupervisionTeamController(AuditSupervisionTeamService service) {
        this.service = service;
    }

    @GetMapping("/api/audit/plan/engagement/{engagementId}/supervision-team/candidates")
    @PreAuthorize("hasAuthority('PERM_AUDIT.SUPERVISION_TEAM.VIEW')")
    public ApiResponse<List<AuditSupervisionCandidateResponse>> listCandidates(@PathVariable UUID engagementId) {
        return ApiResponse.ok(service.listCandidates(engagementId));
    }

    @PutMapping("/api/audit/plan/engagement/{engagementId}/supervision-team")
    @PreAuthorize("hasAuthority('PERM_AUDIT.SUPERVISION_TEAM.MANAGE')")
    public ApiResponse<List<AuditSupervisionTeamMemberResponse>> saveTeam(@PathVariable UUID engagementId,
                                                                            @Valid @RequestBody SaveSupervisionTeamRequest request,
                                                                            @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.saveTeam(engagementId, request, principal));
    }

    @GetMapping("/api/audit/plan/engagement/{engagementId}/supervision-team")
    @PreAuthorize("hasAuthority('PERM_AUDIT.SUPERVISION_TEAM.VIEW')")
    public ApiResponse<List<AuditSupervisionTeamMemberResponse>> listTeam(@PathVariable UUID engagementId) {
        return ApiResponse.ok(service.listTeam(engagementId));
    }

    @GetMapping("/api/audit/plan/engagement/{engagementId}/supervision-team/my-evaluation")
    @PreAuthorize("hasAuthority('PERM_AUDIT.SUPERVISION_TEAM.EVALUATE')")
    public ApiResponse<AuditSupervisionEvaluationResponse> getMyEvaluation(@PathVariable UUID engagementId,
                                                                             @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.getMyEvaluation(engagementId, principal));
    }

    @PutMapping("/api/audit/plan/engagement/{engagementId}/supervision-team/my-evaluation")
    @PreAuthorize("hasAuthority('PERM_AUDIT.SUPERVISION_TEAM.EVALUATE')")
    public ApiResponse<AuditSupervisionEvaluationResponse> saveMyEvaluation(@PathVariable UUID engagementId,
                                                                              @Valid @RequestBody SaveSupervisionEvaluationRequest request,
                                                                              @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.saveMyEvaluation(engagementId, request, principal));
    }

    @GetMapping("/api/audit/plan/engagement/{engagementId}/supervision-team/evaluations")
    @PreAuthorize("hasAuthority('PERM_AUDIT.SUPERVISION_TEAM.VIEW')")
    public ApiResponse<List<AuditSupervisionEvaluationResponse>> listAllEvaluations(@PathVariable UUID engagementId,
                                                                                      @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.listAllEvaluations(engagementId, principal));
    }

    @GetMapping("/api/audit/supervision-team/my-engagements")
    @PreAuthorize("hasAuthority('PERM_AUDIT.SUPERVISION_TEAM.VIEW')")
    public ApiResponse<List<AuditEngagementMonitoringResponse>> listMyEngagements(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.listMyEngagements(principal));
    }
}
