package com.govia.audit.planengagement.progressreport.repository;

import com.govia.audit.planengagement.progressreport.entity.AuditProgressReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditProgressReportRepository extends JpaRepository<AuditProgressReport, UUID> {
    List<AuditProgressReport> findByTenantIdAndEngagementIdOrderByReportDateDesc(UUID tenantId, UUID engagementId);

    /** Dung de tinh "Lan bao cao" tiep theo cho dung to hop engagement+mang NV+nguoi bao cao. */
    long countByTenantIdAndEngagementIdAndBusinessSegmentIdAndReportedEmployeeId(
            UUID tenantId, UUID engagementId, UUID businessSegmentId, UUID reportedEmployeeId);
}
