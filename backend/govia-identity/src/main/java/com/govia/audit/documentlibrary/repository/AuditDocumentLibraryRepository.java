package com.govia.audit.documentlibrary.repository;

import com.govia.audit.documentlibrary.entity.AuditDocumentLibrary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditDocumentLibraryRepository extends JpaRepository<AuditDocumentLibrary, UUID> {
    List<AuditDocumentLibrary> findByTenantIdOrderByDocumentNumberAsc(UUID tenantId);

    Optional<AuditDocumentLibrary> findByTenantIdAndDocumentNumber(UUID tenantId, String documentNumber);
}
