package com.govia.audit.agent.llm;

import java.util.List;

/**
 * Abstraction duy nhat ma Agent Core (AgentOrchestratorService) phu thuoc - khong bao gio goi thang
 * SDK/API cua 1 nha cung cap cu the. Doi provider (vd them OpenAIProvider/ClaudeProvider sau nay khi
 * can) chi can implement interface nay, khong dung toi Agent Core.
 */
public interface LlmProvider {

    /** Goi 1 luot chat - tra ve hoac cau tra loi cuoi, hoac danh sach tool model muon goi. */
    ChatResult chat(List<ChatMessage> messages, List<ToolSpec> tools);

    /** Health check - dung cho GET /api/audit/agent/health, khong duoc goi trong luong chat chinh. */
    boolean isAvailable();

    /** Ten model dang dung, hien trong AgentMetadata.model cua moi cau tra loi. */
    String modelId();
}
