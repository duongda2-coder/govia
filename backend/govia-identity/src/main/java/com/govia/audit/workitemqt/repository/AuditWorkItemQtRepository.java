package com.govia.audit.workitemqt.repository;

import com.govia.audit.workitemqt.entity.AuditWorkItemQt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditWorkItemQtRepository extends JpaRepository<AuditWorkItemQt, UUID> {
    List<AuditWorkItemQt> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<AuditWorkItemQt> findByTenantIdAndCode(UUID tenantId, String code);
}
