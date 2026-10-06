package com.govia.audit.agent.controller;

import com.govia.audit.agent.dto.KnowledgeHit;
import com.govia.audit.agent.service.AgentKnowledgeService;
import com.govia.core.web.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint read-only cho cac tool cua agent khong thuoc mang rui ro (xem audit-tools-contract.md). Giu
 * cung nguyen tac voi AuditToolsController: moi tool di qua @PreAuthorize cua quyen nghiep vu tuong
 * ung - tim van ban can dung quyen xem Thu vien tai lieu, khong phai quyen dung AI.
 */
@RestController
@RequestMapping("/api/audit/agent/tools")
public class AgentToolsController {

    private final AgentKnowledgeService knowledgeService;

    public AgentToolsController(AgentKnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping("/documents")
    @PreAuthorize("hasAuthority('PERM_AUDIT.DOCUMENT_LIBRARY.VIEW')")
    public ApiResponse<List<KnowledgeHit>> searchDocuments(@RequestParam String query,
                                                           @RequestParam(required = false) Integer limit,
                                                           @RequestParam(required = false) Boolean includeExpired) {
        return ApiResponse.ok(knowledgeService.searchDocuments(query, limit, includeExpired));
    }
}
