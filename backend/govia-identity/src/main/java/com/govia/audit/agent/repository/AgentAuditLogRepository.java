package com.govia.audit.agent.repository;

import com.govia.audit.agent.entity.AgentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentAuditLogRepository extends JpaRepository<AgentAuditLog, UUID> {
    List<AgentAuditLog> findByTenantIdAndConversationIdOrderByTurnSeqAsc(UUID tenantId, UUID conversationId);
}
