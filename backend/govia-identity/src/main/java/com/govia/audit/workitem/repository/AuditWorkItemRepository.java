package com.govia.audit.workitem.repository;

import com.govia.audit.workitem.entity.AuditWorkItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditWorkItemRepository extends JpaRepository<AuditWorkItem, UUID> {
    List<AuditWorkItem> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<AuditWorkItem> findByTenantIdAndCode(UUID tenantId, String code);

    /** Dung de tinh "cong viec du dieu kien" cua 1 thanh vien nhom, theo cac nghiep vu 1/2/3 duoc giao. */
    List<AuditWorkItem> findByTenantIdAndActiveTrueAndBusinessSegmentIdIn(UUID tenantId, List<UUID> businessSegmentIds);
}
