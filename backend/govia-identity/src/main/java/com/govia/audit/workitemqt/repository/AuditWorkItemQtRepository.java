package com.govia.audit.workitemqt.repository;

import com.govia.audit.workitemqt.entity.AuditWorkItemQt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditWorkItemQtRepository extends JpaRepository<AuditWorkItemQt, UUID> {
    List<AuditWorkItemQt> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<AuditWorkItemQt> findByTenantIdAndCodeAndApplicableYearAndWorkSetCode(
            UUID tenantId, String code, Integer applicableYear, String workSetCode);

    /** Dung de tinh "cong viec du dieu kien" cua 1 thanh vien nhom trong CKT quy trinh: theo cac
     * nghiep vu 1/2/3 duoc giao VA dung "bo cong viec" (workSetCode) da khai bao luc tao CKT quy
     * trinh cha - xem AuditEngagementTeamService.eligibleWorkItems(). */
    List<AuditWorkItemQt> findByTenantIdAndActiveTrueAndBusinessSegmentIdInAndWorkSetCode(
            UUID tenantId, List<UUID> businessSegmentIds, String workSetCode);
}
