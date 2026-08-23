package com.govia.identity.workflow.controller;

import com.govia.core.web.ApiResponse;
import com.govia.identity.workflow.dto.ProcessDefinitionSummary;
import com.govia.identity.workflow.service.ProcessDefinitionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/workflow/process-definitions")
public class ProcessDefinitionController {

    private final ProcessDefinitionService processDefinitionService;

    public ProcessDefinitionController(ProcessDefinitionService processDefinitionService) {
        this.processDefinitionService = processDefinitionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.DEFINITION.VIEW')")
    public ApiResponse<List<ProcessDefinitionSummary>> list() {
        return ApiResponse.ok(processDefinitionService.list());
    }

    /** Versioning: toan bo phien ban da deploy cho 1 process key. */
    @GetMapping("/{key}/versions")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.DEFINITION.VIEW')")
    public ApiResponse<List<ProcessDefinitionSummary>> versions(@PathVariable String key) {
        return ApiResponse.ok(processDefinitionService.listVersions(key));
    }

    @PostMapping("/deploy")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.DEFINITION.DEPLOY')")
    public ApiResponse<ProcessDefinitionSummary> deploy(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(processDefinitionService.deploy(file));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.DEFINITION.DEPLOY')")
    public ApiResponse<Void> suspend(@PathVariable String id) {
        processDefinitionService.suspend(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.DEFINITION.DEPLOY')")
    public ApiResponse<Void> activate(@PathVariable String id) {
        processDefinitionService.activate(id);
        return ApiResponse.ok(null);
    }
}
