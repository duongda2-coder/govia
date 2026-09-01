package com.govia.audit.branchstaff.repository;

import com.govia.audit.branchstaff.entity.AuditBranchStaff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditBranchStaffRepository extends JpaRepository<AuditBranchStaff, UUID> {
    List<AuditBranchStaff> findByTenantIdOrderByBranchCodeAscStaffNameAsc(UUID tenantId);

    Optional<AuditBranchStaff> findByTenantIdAndBranchCodeAndStaffName(UUID tenantId, String branchCode, String staffName);
}
