package com.govia.audit.agent.llm;

import java.util.Map;

/** Dinh nghia 1 tool de gui cho LLM lam function-calling schema. parametersJsonSchema la 1 JSON
 * Schema object tho (type/properties/required...), serialize thang vao request cua provider - xem
 * AuditToolRegistry de biet danh sach tool that (khop docs/kien-truc-ky-thuat/audit-tools-contract.md). */
public record ToolSpec(String name, String description, Map<String, Object> parametersJsonSchema) {
}
