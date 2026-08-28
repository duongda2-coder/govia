package com.govia.audit.masterdata.repository;

import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditMasterDataItemRepository extends JpaRepository<AuditMasterDataItem, UUID> {
    List<AuditMasterDataItem> findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(UUID tenantId, AuditMasterDataCategory category);

    Optional<AuditMasterDataItem> findByTenantIdAndCategoryAndCode(UUID tenantId, AuditMasterDataCategory category, String code);

    Optional<AuditMasterDataItem> findByTenantIdAndCategoryAndNameIgnoreCase(UUID tenantId, AuditMasterDataCategory category, String name);
}
