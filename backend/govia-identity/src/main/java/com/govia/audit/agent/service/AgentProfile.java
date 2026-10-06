package com.govia.audit.agent.service;

import java.util.List;

/**
 * 1 agent chuyen trach: ma (A0, A1...), ten hien thi, pham vi (dua vao system prompt) va DANH SACH
 * TOOL duoc phep goi - agent chi thay dung bo tool cua minh (prompt ngan hon, model chon tool chinh xac
 * hon), va AgentOrchestratorService chan cung moi tool ngoai danh sach du model co "goi lieu".
 * routingKeywords: cum tu (khong dau cung duoc) de AgentRouter nhan ra cau hoi thuoc agent nay.
 */
public record AgentProfile(
        String code,
        String name,
        String description,
        String scopePrompt,
        List<String> toolNames,
        List<String> routingKeywords,
        List<String> routingPathPrefixes
) {
}
