package com.govia.identity.workflow.controller;

import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.ApiResponse;
import com.govia.identity.workflow.dto.CompleteTaskRequest;
import com.govia.identity.workflow.dto.CreateSubtaskRequest;
import com.govia.identity.workflow.dto.DelegateTaskRequest;
import com.govia.identity.workflow.dto.ReassignTaskRequest;
import com.govia.identity.workflow.dto.SetDueDateRequest;
import com.govia.identity.workflow.dto.TaskSummary;
import com.govia.identity.workflow.service.WorkflowTaskService;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/workflow/tasks")
public class WorkflowTaskController {

    private final WorkflowTaskService workflowTaskService;

    public WorkflowTaskController(WorkflowTaskService workflowTaskService) {
        this.workflowTaskService = workflowTaskService;
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.TASK.VIEW')")
    public ApiResponse<List<TaskSummary>> myTasks(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(workflowTaskService.myTasks(principal));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.TASK.VIEW_ALL')")
    public ApiResponse<List<TaskSummary>> allTasks() {
        return ApiResponse.ok(workflowTaskService.allTasks());
    }

    /** SLA: task da qua han xu ly, chua hoan tat. */
    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.TASK.VIEW_ALL')")
    public ApiResponse<List<TaskSummary>> overdue() {
        return ApiResponse.ok(workflowTaskService.overdueTasks());
    }

    @PostMapping("/{id}/claim")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.TASK.COMPLETE')")
    public ApiResponse<Void> claim(@AuthenticationPrincipal CurrentUserPrincipal principal, @PathVariable String id) {
        workflowTaskService.claim(id, principal);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.TASK.COMPLETE')")
    public ApiResponse<Void> complete(@AuthenticationPrincipal CurrentUserPrincipal principal, @PathVariable String id,
                                       @RequestBody(required = false) CompleteTaskRequest request) {
        workflowTaskService.complete(id, request != null ? request : new CompleteTaskRequest(null), principal);
        return ApiResponse.ok(null);
    }

    /** SLA: dat/xoa han xu ly cho 1 task. */
    @PutMapping("/{id}/due-date")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.TASK.COMPLETE')")
    public ApiResponse<Void> setDueDate(@PathVariable String id, @RequestBody SetDueDateRequest request) {
        workflowTaskService.setDueDate(id, request.dueDate());
        return ApiResponse.ok(null);
    }

    /** Forward: chuyen tiep task cho nguoi khac han, khong quay lai nguoi cu. */
    @PostMapping("/{id}/reassign")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.TASK.COMPLETE')")
    public ApiResponse<Void> reassign(@AuthenticationPrincipal CurrentUserPrincipal principal, @PathVariable String id,
                                        @Valid @RequestBody ReassignTaskRequest request) {
        workflowTaskService.reassign(id, request.assigneeUserId(), principal);
        return ApiResponse.ok(null);
    }

    /** Uy quyen: giao tam thoi, se quay lai owner khi nguoi duoc uy quyen goi /resolve. */
    @PostMapping("/{id}/delegate")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.TASK.COMPLETE')")
    public ApiResponse<Void> delegate(@AuthenticationPrincipal CurrentUserPrincipal principal, @PathVariable String id,
                                        @Valid @RequestBody DelegateTaskRequest request) {
        workflowTaskService.delegate(id, request.delegateUserId(), principal);
        return ApiResponse.ok(null);
    }

    /** Nguoi duoc uy quyen bao da xong phan cua minh - task quay ve owner, owner van phai tu complete. */
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.TASK.COMPLETE')")
    public ApiResponse<Void> resolve(@AuthenticationPrincipal CurrentUserPrincipal principal, @PathVariable String id,
                                       @RequestBody(required = false) CompleteTaskRequest request) {
        workflowTaskService.resolve(id, request != null ? request.variables() : null, principal);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/subtasks")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.TASK.COMPLETE')")
    public ApiResponse<TaskSummary> createSubtask(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                                    @PathVariable String id, @Valid @RequestBody CreateSubtaskRequest request) {
        return ApiResponse.ok(workflowTaskService.createSubtask(id, request.name(), principal));
    }

    @GetMapping("/{id}/subtasks")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.TASK.VIEW')")
    public ApiResponse<List<TaskSummary>> listSubtasks(@PathVariable String id) {
        return ApiResponse.ok(workflowTaskService.listSubtasks(id));
    }
}
