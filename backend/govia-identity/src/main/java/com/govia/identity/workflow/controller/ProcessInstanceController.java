package com.govia.identity.workflow.controller;

import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.ApiResponse;
import com.govia.identity.workflow.dto.ProcessInstanceHistoryDetail;
import com.govia.identity.workflow.dto.ProcessInstanceSummary;
import com.govia.identity.workflow.dto.StartProcessRequest;
import com.govia.identity.workflow.service.ProcessInstanceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workflow/instances")
public class ProcessInstanceController {

    private final ProcessInstanceService processInstanceService;

    public ProcessInstanceController(ProcessInstanceService processInstanceService) {
        this.processInstanceService = processInstanceService;
    }

    @PostMapping("/start")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.INSTANCE.START')")
    public ApiResponse<ProcessInstanceSummary> start(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                                       @Valid @RequestBody StartProcessRequest request) {
        return ApiResponse.ok(processInstanceService.start(request, principal));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.INSTANCE.VIEW')")
    public ApiResponse<List<ProcessInstanceSummary>> list() {
        return ApiResponse.ok(processInstanceService.list());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.INSTANCE.CANCEL')")
    public ApiResponse<Void> cancel(@PathVariable String id, @RequestParam(required = false) String reason) {
        processInstanceService.cancel(id, reason);
        return ApiResponse.ok(null);
    }

    /** History + Audit: chi tiet hoat dong da chay qua + bien luu lai cua 1 process instance. */
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.INSTANCE.VIEW')")
    public ApiResponse<ProcessInstanceHistoryDetail> history(@PathVariable String id) {
        return ApiResponse.ok(processInstanceService.history(id));
    }
}
