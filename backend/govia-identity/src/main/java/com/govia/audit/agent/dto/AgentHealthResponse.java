package com.govia.audit.agent.dto;

import java.util.List;

/** enabled=false: AI dang bi tat bang cau hinh (govia.agent.enabled) - frontend an han nut AI.
 * llmReachable=false: AI bat nhung may chay model khong phan hoi - frontend bao "tam ngung".
 * scheduledSuggestions: G4 - job "Gợi ý AI" theo lich dang bat.
 * fileReading: Cong 3 - AI duoc doc noi dung file dinh kem (mac dinh tat, bat sau khi ATTT duyet). */
public record AgentHealthResponse(boolean enabled, boolean llmReachable, String model, String provider,
                                  boolean semanticSearch, boolean fileReading, boolean scheduledSuggestions,
                                  List<AgentInfo> agents) {

    public record AgentInfo(String code, String name, String description, boolean enabled) {
    }
}
