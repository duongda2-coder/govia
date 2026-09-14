package com.govia.audit.khkt.th.repository;

import com.govia.audit.khkt.th.entity.AuditKhktThCandidateSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AuditKhktThCandidateSegmentRepository extends JpaRepository<AuditKhktThCandidateSegment, UUID> {
    List<AuditKhktThCandidateSegment> findByTenantIdAndCandidateIdIn(UUID tenantId, Collection<UUID> candidateIds);

    /** @Modifying bulk delete (khong qua persistence context) - xem giai thich chi tiet o
     * AuditKhktBpCandidateSegmentRepository (bug da gap: Hibernate flush INSERT truoc DELETE). */
    @Modifying
    @Query("delete from AuditKhktThCandidateSegment s where s.tenantId = :tenantId and s.candidateId = :candidateId")
    void deleteByTenantIdAndCandidateId(UUID tenantId, UUID candidateId);
}
