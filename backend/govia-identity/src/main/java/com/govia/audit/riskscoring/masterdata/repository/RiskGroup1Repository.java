package com.govia.audit.riskscoring.masterdata.repository;

import com.govia.audit.riskscoring.masterdata.entity.RiskGroup1;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskGroup1Repository extends JpaRepository<RiskGroup1, UUID> {
    List<RiskGroup1> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<RiskGroup1> findByTenantIdAndAuditObjectCategoryIdAndCode(UUID tenantId, UUID auditObjectCategoryId, String code);
}
