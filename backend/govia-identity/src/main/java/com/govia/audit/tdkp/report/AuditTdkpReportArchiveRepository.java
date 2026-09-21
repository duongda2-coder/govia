package com.govia.audit.tdkp.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditTdkpReportArchiveRepository extends JpaRepository<AuditTdkpReportArchive, UUID> {
    List<AuditTdkpReportArchive> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
