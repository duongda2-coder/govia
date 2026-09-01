package com.govia.audit.exceptionmapping.repository;

import com.govia.audit.exceptionmapping.entity.AuditExceptionMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditExceptionMappingRepository extends JpaRepository<AuditExceptionMapping, UUID> {
    List<AuditExceptionMapping> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    Optional<AuditExceptionMapping> findByTenantIdAndProcessStepDetailIdAndExceptionTypeId(UUID tenantId, UUID processStepDetailId, UUID exceptionTypeId);
}
