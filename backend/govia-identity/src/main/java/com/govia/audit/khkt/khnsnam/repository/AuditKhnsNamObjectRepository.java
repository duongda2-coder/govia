package com.govia.audit.khkt.khnsnam.repository;

import com.govia.audit.khkt.khnsnam.entity.AuditKhnsNamObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AuditKhnsNamObjectRepository extends JpaRepository<AuditKhnsNamObject, UUID> {
    List<AuditKhnsNamObject> findByTenantIdAndKhnsNamIdIn(UUID tenantId, Collection<UUID> khnsNamIds);

    @Modifying
    @Query("delete from AuditKhnsNamObject o where o.tenantId = :tenantId and o.khnsNamId = :khnsNamId")
    void deleteByTenantIdAndKhnsNamId(UUID tenantId, UUID khnsNamId);
}
