package com.govia.audit.agent.dto;

import java.util.List;

/** enabled=false: AI dang bi tat bang cau hinh (govia.agent.enabled) - frontend an han nut AI.
 * llmReachable=false: AI bat nhung may chay model khong phan hoi - frontend bao "tam ngung". */
public record AgentHealthResponse(boolean enabled, boolean llmReachable, String model, String provider,
                                  boolean semanticSearch, List<AgentInfo> agents) {

    public record AgentInfo(String code, String name, String description, boolean enabled) {
    }
}
