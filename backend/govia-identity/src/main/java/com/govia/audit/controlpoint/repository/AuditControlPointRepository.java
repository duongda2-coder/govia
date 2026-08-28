package com.govia.audit.controlpoint.repository;

import com.govia.audit.controlpoint.entity.AuditControlPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditControlPointRepository extends JpaRepository<AuditControlPoint, UUID> {
    List<AuditControlPoint> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<AuditControlPoint> findByTenantIdAndCode(UUID tenantId, String code);
}
