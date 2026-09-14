package com.govia.audit.khkt.scale.repository;

import com.govia.audit.khkt.scale.entity.AuditKhktScale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditKhktScaleRepository extends JpaRepository<AuditKhktScale, UUID> {
    List<AuditKhktScale> findByTenantIdOrderBySortOrderAsc(UUID tenantId);

    Optional<AuditKhktScale> findByTenantIdAndSortOrder(UUID tenantId, Integer sortOrder);
}
