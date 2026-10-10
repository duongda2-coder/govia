package com.govia.audit.agent.repository;

import com.govia.audit.agent.entity.AgentMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentMessageRepository extends JpaRepository<AgentMessage, UUID> {

    List<AgentMessage> findByTenantIdAndConversationIdOrderBySeqAsc(UUID tenantId, UUID conversationId);

    List<AgentMessage> findByTenantIdAndConversationIdOrderBySeqDesc(UUID tenantId, UUID conversationId, Pageable pageable);

    List<AgentMessage> findByTenantIdAndCreatedAtAfter(UUID tenantId, java.time.Instant after);
}
