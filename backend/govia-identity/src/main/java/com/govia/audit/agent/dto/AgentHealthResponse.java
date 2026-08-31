package com.govia.audit.agent.dto;

public record AgentHealthResponse(boolean ollamaReachable, String model, boolean configured) {
}
