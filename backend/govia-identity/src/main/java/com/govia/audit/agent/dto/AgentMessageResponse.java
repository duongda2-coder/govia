package com.govia.audit.agent.dto;

import java.time.Instant;
import java.util.UUID;

/** 1 tin nhan da luu - role USER thi chi co content; role ASSISTANT co them response (cau tra loi co
 * cau truc, co the da duoc rut gon khi luu neu qua dai - xem ConversationStore). */
public record AgentMessageResponse(UUID id, int seq, String role, String content, String agentCode,
                                   AgentChatResponse response, String pageLabel, Instant createdAt) {
}
