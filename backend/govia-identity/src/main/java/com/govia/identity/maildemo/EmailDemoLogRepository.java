package com.govia.identity.maildemo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailDemoLogRepository extends JpaRepository<EmailDemoLog, UUID> {

    List<EmailDemoLog> findTop100ByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<EmailDemoLog> findByActionToken(String actionToken);
}
