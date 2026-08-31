package com.govia.audit.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** conversationId do frontend tu sinh (giu trong state, mat khi reload trang - xem
 * ConversationStore) de gop nhieu luot hoi/dap thanh 1 hoi thoai co ngu canh. */
public record AgentChatRequest(@NotNull UUID conversationId, @NotBlank String message) {
}
