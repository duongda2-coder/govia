package com.govia.audit.agent.llm;

import java.util.List;

/**
 * 1 message trong hoi thoai gui cho LLM - dung chung cho moi provider (Ollama, sau nay OpenAI/Claude
 * neu them). role: "system" | "user" | "assistant" | "tool". Voi role "assistant" ma model yeu cau
 * goi tool, content co the null va toolCalls chua danh sach yeu cau. Voi role "tool" (ket qua tra ve
 * cho model), toolName ghi lai tool nao da sinh ra content do.
 */
public record ChatMessage(String role, String content, String toolName, List<ToolCallRequest> toolCalls) {

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content, null, null);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content, null, null);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content, null, null);
    }

    public static ChatMessage toolResult(String toolName, String content) {
        return new ChatMessage("tool", content, toolName, null);
    }
}
