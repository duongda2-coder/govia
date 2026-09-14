package com.govia.audit.khkt.bp.repository;

import com.govia.audit.khkt.bp.entity.AuditKhktBpCandidateSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AuditKhktBpCandidateSegmentRepository extends JpaRepository<AuditKhktBpCandidateSegment, UUID> {
    List<AuditKhktBpCandidateSegment> findByTenantIdAndCandidateIdIn(UUID tenantId, Collection<UUID> candidateIds);

    /** @Modifying bulk delete (khong qua persistence context) - bat buoc, vi Hibernate flush
     * INSERT truoc DELETE trong cung 1 transaction bat ke thu tu goi trong code; neu dung
     * deleteByX thuong (find-roi-remove qua persistence context), INSERT lai cung gia tri se
     * dung unique constraint truoc khi DELETE cu duoc flush (da gap loi nay khi test). */
    @Modifying
    @Query("delete from AuditKhktBpCandidateSegment s where s.tenantId = :tenantId and s.candidateId = :candidateId")
    void deleteByTenantIdAndCandidateId(UUID tenantId, UUID candidateId);
}
