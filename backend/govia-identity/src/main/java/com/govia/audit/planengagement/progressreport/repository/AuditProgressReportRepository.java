package com.govia.audit.planengagement.progressreport.repository;

import com.govia.audit.planengagement.progressreport.entity.AuditProgressReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditProgressReportRepository extends JpaRepository<AuditProgressReport, UUID> {
    List<AuditProgressReport> findByTenantIdAndEngagementIdOrderByReportDateDesc(UUID tenantId, UUID engagementId);

    /** Neu to hop nay da co bao cao trong ngay hom nay thi lay lai cung 1 "Lan bao cao", khong tang. */
    Optional<AuditProgressReport> findFirstByTenantIdAndEngagementIdAndBusinessSegmentIdAndReportedEmployeeIdAndReportDate(
            UUID tenantId, UUID engagementId, UUID businessSegmentId, UUID reportedEmployeeId, LocalDate reportDate);

    /** Dung de tinh "Lan bao cao" tiep theo (dem so NGAY khac nhau da bao cao, khong dem so lan
     * upload) cho dung to hop engagement+mang NV+nguoi bao cao. */
    @Query("select count(distinct r.reportDate) from AuditProgressReport r where r.tenantId = :tenantId "
            + "and r.engagementId = :engagementId "
            + "and (r.businessSegmentId = :businessSegmentId or (:businessSegmentId is null and r.businessSegmentId is null)) "
            + "and r.reportedEmployeeId = :reportedEmployeeId")
    long countDistinctReportDateByTenantIdAndEngagementIdAndBusinessSegmentIdAndReportedEmployeeId(
            @Param("tenantId") UUID tenantId, @Param("engagementId") UUID engagementId,
            @Param("businessSegmentId") UUID businessSegmentId, @Param("reportedEmployeeId") UUID reportedEmployeeId);
}
