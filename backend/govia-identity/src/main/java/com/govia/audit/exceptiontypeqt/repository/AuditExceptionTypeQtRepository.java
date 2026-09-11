package com.govia.audit.exceptiontypeqt.repository;

import com.govia.audit.exceptiontypeqt.entity.AuditExceptionTypeQt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditExceptionTypeQtRepository extends JpaRepository<AuditExceptionTypeQt, UUID> {
    List<AuditExceptionTypeQt> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<AuditExceptionTypeQt> findByTenantIdAndCodeAndApplicableYear(UUID tenantId, String code, Integer applicableYear);
}
