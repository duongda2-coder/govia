package com.govia.audit.agent.controller;

import com.govia.audit.agent.dto.AgentChatRequest;
import com.govia.audit.agent.dto.AgentChatResponse;
import com.govia.audit.agent.dto.AgentHealthResponse;
import com.govia.audit.agent.llm.LlmProvider;
import com.govia.audit.agent.service.AgentOrchestratorService;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Audit AI Assistant - man chat cua kiem toan vien. Yeu cau JWT hop le + quyen AUDIT.AGENT.VIEW nhu
 * moi API khac; MOI tool ma agent goi ben trong van di qua quyen rieng cua tool do (xem
 * AuditToolExecutor - goi thang method cua AuditToolsController, van bi @PreAuthorize cua controller
 * do chan dung nhu khi goi qua HTTP) - endpoint nay khong cap them quyen gi cho user ngoai nhung gi
 * ho da co san.
 */
@RestController
@RequestMapping("/api/audit/agent")
public class AgentController {

    private final AgentOrchestratorService orchestrator;
    private final LlmProvider llmProvider;

    public AgentController(AgentOrchestratorService orchestrator, LlmProvider llmProvider) {
        this.orchestrator = orchestrator;
        this.llmProvider = llmProvider;
    }

    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.VIEW')")
    public ApiResponse<AgentChatResponse> chat(@Valid @RequestBody AgentChatRequest request,
                                                @AuthenticationPrincipal CurrentUserPrincipal principal) {
        AgentChatResponse response = orchestrator.chat(request.conversationId(), request.message(), principal.userId());
        return ApiResponse.ok(response);
    }

    @GetMapping("/health")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.VIEW')")
    public ApiResponse<AgentHealthResponse> health() {
        boolean reachable = llmProvider.isAvailable();
        return ApiResponse.ok(new AgentHealthResponse(reachable, llmProvider.modelId(), true));
    }
}
