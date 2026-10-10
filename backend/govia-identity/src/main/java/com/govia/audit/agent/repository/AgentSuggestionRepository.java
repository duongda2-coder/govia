package com.govia.audit.agent.repository;

import com.govia.audit.agent.entity.AgentSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentSuggestionRepository extends JpaRepository<AgentSuggestion, UUID> {

    List<AgentSuggestion> findByTenantIdAndUserIdOrderByCreatedAtDesc(UUID tenantId, UUID userId);

    Optional<AgentSuggestion> findByIdAndTenantIdAndUserId(UUID id, UUID tenantId, UUID userId);

    long countByTenantIdAndCreatedAtAfter(UUID tenantId, Instant after);
}
