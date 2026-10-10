package com.govia.audit.agent.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** 1 "Gợi ý AI" - linkPath la man hinh nghiep vu lien quan (nguoi dung tu mo va xu ly theo cach hien tai). */
public record AgentSuggestionResponse(UUID id, String agentCode, String category, String severity, String title, String detail,
                                      String linkPath, int itemCount, LocalDate runDate, Instant createdAt, boolean read) {
}
