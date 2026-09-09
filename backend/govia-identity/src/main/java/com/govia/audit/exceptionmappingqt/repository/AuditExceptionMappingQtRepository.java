package com.govia.audit.exceptionmappingqt.repository;

import com.govia.audit.exceptionmappingqt.entity.AuditExceptionMappingQt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditExceptionMappingQtRepository extends JpaRepository<AuditExceptionMappingQt, UUID> {
    List<AuditExceptionMappingQt> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    Optional<AuditExceptionMappingQt> findByTenantIdAndProcessStepDetailIdAndExceptionTypeId(
            UUID tenantId, UUID processStepDetailId, UUID exceptionTypeId);
}
