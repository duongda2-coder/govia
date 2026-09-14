package com.govia.audit.riskscoring.masterdata.repository;

import com.govia.audit.riskscoring.masterdata.entity.AuditInspectionType;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectInspectionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AuditObjectInspectionHistoryRepository extends JpaRepository<AuditObjectInspectionHistory, UUID> {
    List<AuditObjectInspectionHistory> findByTenantIdAndAuditObjectUnitIdOrderByYearDesc(UUID tenantId, UUID auditObjectUnitId);

    /** Dung de module KHKT tinh cac cot "T-5..T-1": lay 1 lan tat ca lich su cua nhieu don vi trong 1 khoang nam. */
    List<AuditObjectInspectionHistory> findByTenantIdAndAuditObjectUnitIdInAndYearBetween(
            UUID tenantId, Collection<UUID> auditObjectUnitIds, Integer fromYear, Integer toYear);
}
