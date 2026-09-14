package com.govia.audit.khkt.th.repository;

import com.govia.audit.khkt.th.entity.AuditKhktThConfirmedSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AuditKhktThConfirmedSegmentRepository extends JpaRepository<AuditKhktThConfirmedSegment, UUID> {
    List<AuditKhktThConfirmedSegment> findByTenantIdAndConfirmedIdIn(UUID tenantId, Collection<UUID> confirmedIds);

    @Modifying
    @Query("delete from AuditKhktThConfirmedSegment s where s.tenantId = :tenantId and s.confirmedId = :confirmedId")
    void deleteByTenantIdAndConfirmedId(UUID tenantId, UUID confirmedId);
}
