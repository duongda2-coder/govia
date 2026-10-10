package com.govia.audit.agent.controller;

import com.govia.audit.agent.config.AgentProperties;
import com.govia.audit.agent.config.LlmProperties;
import com.govia.audit.agent.dto.AgentChatRequest;
import com.govia.audit.agent.dto.AgentChatResponse;
import com.govia.audit.agent.dto.AgentConversationSummary;
import com.govia.audit.agent.dto.AgentHealthResponse;
import com.govia.audit.agent.dto.AgentMessageResponse;
import com.govia.audit.agent.llm.EmbeddingClient;
import com.govia.audit.agent.llm.LlmProvider;
import com.govia.audit.agent.service.AgentFileService;
import com.govia.audit.agent.service.AgentOrchestratorService;
import com.govia.audit.agent.service.AgentProfileRegistry;
import com.govia.audit.agent.service.ConversationStore;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Audit AI Assistant - khung chat (nut noi tren moi man hinh). Yeu cau JWT hop le + quyen
 * AUDIT.AGENT.VIEW nhu moi API khac; MOI tool ma agent goi ben trong van di qua quyen rieng cua tool
 * do (xem AuditToolExecutor - goi thang method cua controller tuong ung, van bi @PreAuthorize chan
 * dung nhu khi goi qua HTTP) - endpoint nay khong cap them quyen gi cho user ngoai nhung gi ho da co.
 * Hoi thoai chi doc/an duoc boi chinh nguoi da tao (ConversationStore kiem tra chu so huu).
 */
@RestController
@RequestMapping("/api/audit/agent")
public class AgentController {

    private final AgentOrchestratorService orchestrator;
    private final LlmProvider llmProvider;
    private final ConversationStore conversationStore;
    private final AgentProfileRegistry profileRegistry;
    private final AgentProperties agentProperties;
    private final LlmProperties llmProperties;
    private final EmbeddingClient embeddingClient;
    private final AgentFileService fileService;

    public AgentController(AgentOrchestratorService orchestrator, LlmProvider llmProvider, ConversationStore conversationStore,
                           AgentProfileRegistry profileRegistry, AgentProperties agentProperties, LlmProperties llmProperties,
                           EmbeddingClient embeddingClient, AgentFileService fileService) {
        this.fileService = fileService;
        this.orchestrator = orchestrator;
        this.llmProvider = llmProvider;
        this.conversationStore = conversationStore;
        this.profileRegistry = profileRegistry;
        this.agentProperties = agentProperties;
        this.llmProperties = llmProperties;
        this.embeddingClient = embeddingClient;
    }

    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.VIEW')")
    public ApiResponse<AgentChatResponse> chat(@Valid @RequestBody AgentChatRequest request,
                                                @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(orchestrator.chat(request, principal.userId()));
    }

    /** Khong goi model khi AI dang tat - tranh lam cham man hinh khi may chay model khong co. */
    @GetMapping("/health")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.VIEW')")
    public ApiResponse<AgentHealthResponse> health() {
        boolean enabled = agentProperties.isEnabled();
        List<AgentHealthResponse.AgentInfo> agents = profileRegistry.all().stream()
                .map(p -> new AgentHealthResponse.AgentInfo(p.code(), p.name(), p.description(), profileRegistry.isEnabled(p)))
                .toList();
        return ApiResponse.ok(new AgentHealthResponse(enabled, enabled && llmProvider.isAvailable(), llmProvider.modelId(),
                llmProperties.getProvider(), embeddingClient.isConfigured(), fileService.enabled(),
                enabled && agentProperties.getSchedule().isEnabled(), agents));
    }

    @GetMapping("/conversations")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.VIEW')")
    public ApiResponse<List<AgentConversationSummary>> conversations(@RequestParam(required = false) Integer limit,
                                                                     @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(conversationStore.listForUser(principal.userId(), limit));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.VIEW')")
    public ApiResponse<List<AgentMessageResponse>> messages(@PathVariable UUID conversationId,
                                                            @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(conversationStore.messages(conversationId, principal.userId()));
    }

    @DeleteMapping("/conversations/{conversationId}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.AGENT.VIEW')")
    public ApiResponse<Void> archive(@PathVariable UUID conversationId,
                                     @AuthenticationPrincipal CurrentUserPrincipal principal) {
        conversationStore.archive(conversationId, principal.userId());
        return ApiResponse.ok(null);
    }
}
