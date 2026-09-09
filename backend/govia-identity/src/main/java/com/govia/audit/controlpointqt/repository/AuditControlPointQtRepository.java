package com.govia.audit.controlpointqt.repository;

import com.govia.audit.controlpointqt.entity.AuditControlPointQt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditControlPointQtRepository extends JpaRepository<AuditControlPointQt, UUID> {
    List<AuditControlPointQt> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<AuditControlPointQt> findByTenantIdAndCode(UUID tenantId, String code);
}
