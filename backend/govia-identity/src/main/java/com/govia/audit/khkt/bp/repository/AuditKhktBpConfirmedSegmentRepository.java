package com.govia.audit.khkt.bp.repository;

import com.govia.audit.khkt.bp.entity.AuditKhktBpConfirmedSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AuditKhktBpConfirmedSegmentRepository extends JpaRepository<AuditKhktBpConfirmedSegment, UUID> {
    List<AuditKhktBpConfirmedSegment> findByTenantIdAndConfirmedIdIn(UUID tenantId, Collection<UUID> confirmedIds);

    /** @Modifying bulk delete - xem giai thich o AuditKhktBpCandidateSegmentRepository. */
    @Modifying
    @Query("delete from AuditKhktBpConfirmedSegment s where s.tenantId = :tenantId and s.confirmedId = :confirmedId")
    void deleteByTenantIdAndConfirmedId(UUID tenantId, UUID confirmedId);
}
