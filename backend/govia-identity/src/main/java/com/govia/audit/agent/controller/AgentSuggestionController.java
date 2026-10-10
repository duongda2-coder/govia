package com.govia.audit.agent.controller;

import com.govia.audit.agent.dto.AgentSuggestionResponse;
import com.govia.audit.agent.service.AgentKpiService;
import com.govia.audit.agent.service.AgentSuggestionScheduler;
import com.govia.audit.agent.service.AgentSuggestionService;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.ApiResponse;
import com.govia.core.web.BusinessException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * G4 - tab "Gợi ý AI" trong khung Tro ly AI (moi nguoi chi thay goi y cua minh) va thong ke KPI cho quan tri.
 * Khong endpoint nao ghi vao du lieu nghiep vu; "dismiss"/"read" chi doi trang thai dong goi y cua chinh nguoi dung.
 */
@RestController
@RequestMapping("/api/audit/agent")
public class AgentSuggestionController {

    private final AgentSuggestionService suggestionService;
    private final AgentKpiService kpiService;
    private final ObjectProvider<AgentSuggestionScheduler> scheduler;

    public AgentSuggestionController(AgentSuggestionService suggestionService, AgentKpiService kpiService,
                                     ObjectProvider<AgentSuggestionScheduler> scheduler) {
        this.suggestionService = suggestionService;
        this.kpiService = kpiService;
        this.scheduler = scheduler;
    }

    @GetMapping("/suggestions")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.VIEW')")
    public ApiResponse<List<AgentSuggestionResponse>> list(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(suggestionService.list(principal.userId()));
    }

    /** "Làm mới": chay ngay cac kiem tra cho chinh minh (khong doi den lich sang hom sau). */
    @PostMapping("/suggestions/refresh")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.VIEW')")
    public ApiResponse<List<AgentSuggestionResponse>> refresh(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(suggestionService.refresh(principal));
    }

    @PostMapping("/suggestions/{id}/dismiss")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.VIEW')")
    public ApiResponse<Void> dismiss(@PathVariable UUID id, @AuthenticationPrincipal CurrentUserPrincipal principal) {
        suggestionService.dismiss(id, principal.userId());
        return ApiResponse.ok(null);
    }

    @PostMapping("/suggestions/read")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.VIEW')")
    public ApiResponse<Void> markRead(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        suggestionService.markAllRead(principal.userId());
        return ApiResponse.ok(null);
    }

    /** Quan tri: chay ngay job theo lich cho moi nguoi dung (vd sau khi bat lai AI) - moi nguoi van theo quyen cua ho. */
    @PostMapping("/suggestions/run-all")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.ADMIN')")
    public ApiResponse<Map<String, Object>> runAll() {
        AgentSuggestionScheduler job = scheduler.getIfAvailable();
        if (job == null) {
            throw new BusinessException("AGENT_SCHEDULE_DISABLED", "Job Goi y AI theo lich dang tat (GOVIA_AGENT_SCHEDULE_ENABLED=false)",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return ApiResponse.ok(Map.of("users", job.runAll()));
    }

    @GetMapping("/kpi")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.ADMIN')")
    public ApiResponse<Map<String, Object>> kpi(@RequestParam(required = false) Integer days) {
        return ApiResponse.ok(kpiService.kpi(days));
    }
}
