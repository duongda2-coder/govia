package com.govia.audit.processstep.repository;

import com.govia.audit.processstep.entity.AuditProcessStepDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditProcessStepDetailRepository extends JpaRepository<AuditProcessStepDetail, UUID> {
    List<AuditProcessStepDetail> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<AuditProcessStepDetail> findByTenantIdAndCode(UUID tenantId, String code);
}
