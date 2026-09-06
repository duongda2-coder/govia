package com.govia.audit.planengagement.monitoring.controller;

import com.govia.audit.planengagement.monitoring.dto.AuditEngagementMonitoringResponse;
import com.govia.audit.planengagement.monitoring.dto.AuditEngagementTeamMemberDetailResponse;
import com.govia.audit.planengagement.monitoring.dto.TeamMemberScoringRequest;
import com.govia.audit.planengagement.monitoring.dto.TeamRankingUpdateRequest;
import com.govia.audit.planengagement.monitoring.service.AuditEngagementMonitoringService;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Man hinh "Quản lý đợt kiểm toán" (sheet cung ten cua "Tao CKT (2).xlsx") - dashboard giam sat
 * doan kiem toan, tach rieng voi man hinh CRUD "Khoi tao va quan ly cuoc kiem toan"
 * ({@link com.govia.audit.planengagement.controller.AuditEngagementController}) vi doi tuong su
 * dung khac nhau (nguoi giam sat vs nguoi khoi tao CKT). */
@RestController
@RequestMapping("/api/audit/plan/engagement/monitoring")
public class AuditEngagementMonitoringController {

    private final AuditEngagementMonitoringService service;

    public AuditEngagementMonitoringController(AuditEngagementMonitoringService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT.VIEW')")
    public ApiResponse<List<AuditEngagementMonitoringResponse>> list(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.list(principal));
    }

    @GetMapping("/{id}/team-detail")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT.VIEW')")
    public ApiResponse<List<AuditEngagementTeamMemberDetailResponse>> teamDetail(@PathVariable UUID id,
                                                                                  @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.teamDetail(id, principal));
    }

    @PatchMapping("/{id}/team-ranking")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT.EDIT')")
    public ApiResponse<AuditEngagementMonitoringResponse> updateTeamRanking(@PathVariable UUID id, @Valid @RequestBody TeamRankingUpdateRequest request) {
        return ApiResponse.ok(service.updateTeamRanking(id, request));
    }

    @PatchMapping("/{id}/team-members/{memberId}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_TEAM.EDIT')")
    public ApiResponse<AuditEngagementTeamMemberDetailResponse> updateTeamMemberScoring(@PathVariable UUID id, @PathVariable UUID memberId,
                                                                                         @Valid @RequestBody TeamMemberScoringRequest request) {
        return ApiResponse.ok(service.updateTeamMemberScoring(id, memberId, request));
    }
}
