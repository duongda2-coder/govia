package com.govia.audit.agent.dto;

import java.util.List;

/**
 * Response chuan hoa cua Audit AI Agent - FACT (facts, lay truc tiep tu tool) tach biet khoi ANALYSIS
 * (phan tich dua tren facts) va RECOMMENDATION (de xuat cua agent, khong phai fact). Frontend render
 * rieng tung phan, khong parse text bang regex. agentCode/agentName: agent chuyen trach da tra loi
 * luot nay (A0 tro ly chung, A1 rui ro...).
 */
public record AgentChatResponse(
        String answer,
        List<String> facts,
        List<String> analysis,
        List<String> recommendations,
        List<EvidenceRef> evidence,
        AgentMetadata metadata,
        String agentCode,
        String agentName
) {
    public AgentChatResponse withAgent(String code, String name) {
        return new AgentChatResponse(answer, facts, analysis, recommendations, evidence, metadata, code, name);
    }
}
