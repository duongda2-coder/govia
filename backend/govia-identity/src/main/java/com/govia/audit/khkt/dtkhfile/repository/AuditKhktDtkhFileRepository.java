package com.govia.audit.khkt.dtkhfile.repository;

import com.govia.audit.khkt.dtkhfile.entity.AuditKhktDtkhFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditKhktDtkhFileRepository extends JpaRepository<AuditKhktDtkhFile, UUID> {
    List<AuditKhktDtkhFile> findByTenantIdAndYearOrderByCreatedAtDesc(UUID tenantId, Integer year);
}
