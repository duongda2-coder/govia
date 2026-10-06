package com.govia.audit.agent.repository;

import com.govia.audit.agent.entity.AgentConversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentConversationRepository extends JpaRepository<AgentConversation, UUID> {

    Optional<AgentConversation> findByTenantIdAndConversationKey(UUID tenantId, UUID conversationKey);

    List<AgentConversation> findByTenantIdAndUserIdAndArchivedFalseOrderByLastMessageAtDesc(UUID tenantId, UUID userId, Pageable pageable);
}
