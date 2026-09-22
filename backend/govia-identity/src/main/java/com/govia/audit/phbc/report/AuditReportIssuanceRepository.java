package com.govia.audit.phbc.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditReportIssuanceRepository extends JpaRepository<AuditReportIssuance, UUID> {
    List<AuditReportIssuance> findByTenantIdOrderByReportDateDescReportNumberAsc(UUID tenantId);
}
