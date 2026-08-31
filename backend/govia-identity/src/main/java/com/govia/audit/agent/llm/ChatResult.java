package com.govia.audit.agent.llm;

import java.util.List;

/** Ket qua 1 luot goi LLM: hoac model tra loi cuoi (finalMessage khac null, toolCalls rong), hoac
 * model yeu cau goi them tool (toolCalls khac rong). */
public record ChatResult(ChatMessage finalMessage, List<ToolCallRequest> toolCalls) {

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
