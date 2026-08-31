package com.govia.audit.agent.llm;

import java.util.Map;

/** 1 yeu cau goi tool ma model sinh ra. "id" co the null neu provider khong tra id (vd mot so ban
 * Ollama) - AgentOrchestratorService tu sinh id thay the khi can de theo doi trong 1 luot. */
public record ToolCallRequest(String id, String name, Map<String, Object> arguments) {
}
