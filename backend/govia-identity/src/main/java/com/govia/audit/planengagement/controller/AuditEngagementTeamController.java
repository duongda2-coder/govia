package com.govia.audit.planengagement.controller;

import com.govia.audit.planengagement.dto.AssignWorkItemsRequest;
import com.govia.audit.planengagement.dto.AuditEngagementAssignmentResponse;
import com.govia.audit.planengagement.dto.AuditEngagementGroupMemberRequest;
import com.govia.audit.planengagement.dto.AuditEngagementGroupMemberResponse;
import com.govia.audit.planengagement.dto.AuditEngagementGroupRequest;
import com.govia.audit.planengagement.dto.AuditEngagementGroupResponse;
import com.govia.audit.planengagement.dto.EligibleWorkItemResponse;
import com.govia.audit.planengagement.service.AuditEngagementTeamService;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

/** "Danh sach nhom", "Danh sach thanh vien", "Phan cong" (sheet "quan ly DKT" cua Tao CKT.xlsx). */
@RestController
@RequestMapping("/api/audit/plan/engagement/{engagementId}")
public class AuditEngagementTeamController {

    private final AuditEngagementTeamService service;

    public AuditEngagementTeamController(AuditEngagementTeamService service) {
        this.service = service;
    }

    @GetMapping("/groups")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_TEAM.VIEW')")
    public ApiResponse<List<AuditEngagementGroupResponse>> listGroups(@PathVariable UUID engagementId) {
        return ApiResponse.ok(service.listGroups(engagementId));
    }

    @PostMapping("/groups")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_TEAM.CREATE')")
    public ApiResponse<AuditEngagementGroupResponse> addGroup(@PathVariable UUID engagementId, @Valid @RequestBody AuditEngagementGroupRequest request) {
        return ApiResponse.ok(service.addGroup(engagementId, request));
    }

    @DeleteMapping("/groups/{groupId}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_TEAM.DELETE')")
    public ApiResponse<Void> deleteGroup(@PathVariable UUID engagementId, @PathVariable UUID groupId) {
        service.deleteGroup(engagementId, groupId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/members")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_TEAM.VIEW')")
    public ApiResponse<List<AuditEngagementGroupMemberResponse>> listAllMembers(@PathVariable UUID engagementId) {
        return ApiResponse.ok(service.listMembersByEngagement(engagementId));
    }

    @GetMapping("/groups/{groupId}/members")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_TEAM.VIEW')")
    public ApiResponse<List<AuditEngagementGroupMemberResponse>> listMembers(@PathVariable UUID engagementId, @PathVariable UUID groupId) {
        return ApiResponse.ok(service.listMembers(engagementId, groupId));
    }

    @PostMapping("/groups/{groupId}/members")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_TEAM.CREATE')")
    public ApiResponse<AuditEngagementGroupMemberResponse> addMember(@PathVariable UUID engagementId, @PathVariable UUID groupId,
                                                                       @Valid @RequestBody AuditEngagementGroupMemberRequest request) {
        return ApiResponse.ok(service.addMember(engagementId, groupId, request));
    }

    @PutMapping("/groups/{groupId}/members/{memberId}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_TEAM.EDIT')")
    public ApiResponse<AuditEngagementGroupMemberResponse> updateMember(@PathVariable UUID engagementId, @PathVariable UUID groupId,
                                                                          @PathVariable UUID memberId, @Valid @RequestBody AuditEngagementGroupMemberRequest request) {
        return ApiResponse.ok(service.updateMember(engagementId, groupId, memberId, request));
    }

    @DeleteMapping("/groups/{groupId}/members/{memberId}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_TEAM.DELETE')")
    public ApiResponse<Void> deleteMember(@PathVariable UUID engagementId, @PathVariable UUID groupId, @PathVariable UUID memberId) {
        service.deleteMember(engagementId, groupId, memberId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/groups/{groupId}/members/{memberId}/eligible-work-items")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_TEAM.VIEW')")
    public ApiResponse<List<EligibleWorkItemResponse>> listEligibleWorkItems(@PathVariable UUID engagementId, @PathVariable UUID groupId, @PathVariable UUID memberId) {
        return ApiResponse.ok(service.listEligibleWorkItems(engagementId, groupId, memberId));
    }

    @GetMapping("/groups/{groupId}/members/{memberId}/assignments")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_TEAM.VIEW')")
    public ApiResponse<List<AuditEngagementAssignmentResponse>> listAssignments(@PathVariable UUID engagementId, @PathVariable UUID groupId, @PathVariable UUID memberId) {
        return ApiResponse.ok(service.listAssignments(engagementId, groupId, memberId));
    }

    @PostMapping("/groups/{groupId}/members/{memberId}/assignments")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_TEAM.CREATE')")
    public ApiResponse<List<AuditEngagementAssignmentResponse>> assignWorkItems(@PathVariable UUID engagementId, @PathVariable UUID groupId, @PathVariable UUID memberId,
                                                                                  @Valid @RequestBody AssignWorkItemsRequest request,
                                                                                  @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.assignWorkItems(engagementId, groupId, memberId, request, principal.employeeCode()));
    }

    @DeleteMapping("/groups/{groupId}/members/{memberId}/assignments/{assignmentId}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_TEAM.DELETE')")
    public ApiResponse<Void> deleteAssignment(@PathVariable UUID engagementId, @PathVariable UUID groupId, @PathVariable UUID memberId, @PathVariable UUID assignmentId,
                                               @AuthenticationPrincipal CurrentUserPrincipal principal) {
        service.deleteAssignment(engagementId, groupId, memberId, assignmentId, principal.employeeCode());
        return ApiResponse.ok(null);
    }
}
