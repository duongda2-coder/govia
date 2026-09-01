package com.govia.core.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByJti(String jti);

    List<UserSession> findByTenantIdAndUserIdAndStatus(UUID tenantId, UUID userId, UserSessionStatus status);
}
