package com.govia.audit.agent.tools;

import com.govia.audit.agent.llm.ToolSpec;

import java.util.Map;

/**
 * 1 tool that trong AuditToolRegistry - gop ca schema goi cho LLM (name/description/parametersSchema)
 * lan thong tin de AuditToolExecutor thuc thi (path noi bo tren AuditToolsController). "path" la null
 * cho tool dac biet "submit_final_answer" (khong goi HTTP, xem AgentOrchestratorService).
 */
public record AuditToolDefinition(String name, String description, Map<String, Object> parametersSchema, String path) {

    public ToolSpec toToolSpec() {
        return new ToolSpec(name, description, parametersSchema);
    }
}
