package com.govia.audit.exceptiontype.repository;

import com.govia.audit.exceptiontype.entity.AuditExceptionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditExceptionTypeRepository extends JpaRepository<AuditExceptionType, UUID> {
    List<AuditExceptionType> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<AuditExceptionType> findByTenantIdAndCode(UUID tenantId, String code);
}
