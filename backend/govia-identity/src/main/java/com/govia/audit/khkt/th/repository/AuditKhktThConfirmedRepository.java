package com.govia.audit.khkt.th.repository;

import com.govia.audit.khkt.th.entity.AuditKhktThConfirmed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AuditKhktThConfirmedRepository extends JpaRepository<AuditKhktThConfirmed, UUID> {
    List<AuditKhktThConfirmed> findByTenantIdAndYearOrderByAuditObjectCodeAsc(UUID tenantId, Integer year);

    /** @Modifying bulk delete - xem giai thich o AuditKhktBpCandidateSegmentRepository. */
    @Modifying
    @Query("delete from AuditKhktThConfirmed c where c.tenantId = :tenantId and c.year = :year")
    void deleteByTenantIdAndYear(UUID tenantId, Integer year);
}
