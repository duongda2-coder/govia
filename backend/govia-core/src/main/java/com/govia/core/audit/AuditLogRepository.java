package com.govia.core.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByEntityNameAndEntityIdOrderByCreatedAtDesc(String entityName, UUID entityId);
}
