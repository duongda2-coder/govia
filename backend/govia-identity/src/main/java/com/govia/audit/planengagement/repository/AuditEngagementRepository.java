package com.govia.audit.planengagement.repository;

import com.govia.audit.planengagement.entity.AuditEngagement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditEngagementRepository extends JpaRepository<AuditEngagement, UUID> {
    List<AuditEngagement> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<AuditEngagement> findByTenantIdAndCode(UUID tenantId, String code);

    long countByTenantIdAndAuditObjectUnitIdAndYear(UUID tenantId, UUID auditObjectUnitId, Integer year);

    /** Dung de AuditKhktTransferService (ChuyenThongTinKHTH) tim CKT da co cua (nam, doi tuong) de
     * ghi de thay vi tao trung - lay ban ghi CU NHAT neu vi ly do nao do da co nhieu hon 1. */
    Optional<AuditEngagement> findFirstByTenantIdAndAuditObjectUnitIdAndYearOrderByCreatedAtAsc(UUID tenantId, UUID auditObjectUnitId, Integer year);

    List<AuditEngagement> findByTenantIdAndProcessEngagementIdOrderByCreatedAtAsc(UUID tenantId, UUID processEngagementId);

    long countByTenantIdAndProcessEngagementId(UUID tenantId, UUID processEngagementId);
}
