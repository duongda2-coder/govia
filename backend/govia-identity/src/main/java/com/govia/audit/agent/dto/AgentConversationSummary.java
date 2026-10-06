package com.govia.audit.agent.dto;

import java.time.Instant;
import java.util.UUID;

public record AgentConversationSummary(UUID id, String title, String lastAgentCode, Instant lastMessageAt, int messageCount) {
}
