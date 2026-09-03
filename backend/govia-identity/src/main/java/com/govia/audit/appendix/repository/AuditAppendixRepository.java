package com.govia.audit.appendix.repository;

import com.govia.audit.appendix.entity.AuditAppendix;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditAppendixRepository extends JpaRepository<AuditAppendix, UUID> {
    List<AuditAppendix> findByTenantIdOrderByAppendixCodeAsc(UUID tenantId);

    Optional<AuditAppendix> findByTenantIdAndAppendixCode(UUID tenantId, String appendixCode);
}
