package com.govia.audit.khkt.bp.repository;

import com.govia.audit.khkt.bp.entity.AuditKhktBpConfirmed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AuditKhktBpConfirmedRepository extends JpaRepository<AuditKhktBpConfirmed, UUID> {
    List<AuditKhktBpConfirmed> findByTenantIdAndDepartmentIdAndYearOrderByAuditObjectCodeAsc(UUID tenantId, UUID departmentId, Integer year);

    /** Dung de AuditKhktThService#sync tong hop qua TAT CA phong (khong loc theo 1 phong). */
    List<AuditKhktBpConfirmed> findByTenantIdAndYearOrderByAuditObjectCodeAsc(UUID tenantId, Integer year);

    /** @Modifying bulk delete - xem giai thich o AuditKhktBpCandidateSegmentRepository (confirm()
     * xoa roi insert lai trong cung 1 transaction, insert phai thay xoa da xay ra that su). */
    @Modifying
    @Query("delete from AuditKhktBpConfirmed c where c.tenantId = :tenantId and c.departmentId = :departmentId and c.year = :year")
    void deleteByTenantIdAndDepartmentIdAndYear(UUID tenantId, UUID departmentId, Integer year);
}
