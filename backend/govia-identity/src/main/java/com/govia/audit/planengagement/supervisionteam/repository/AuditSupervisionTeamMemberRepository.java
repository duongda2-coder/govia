package com.govia.audit.planengagement.supervisionteam.repository;

import com.govia.audit.planengagement.supervisionteam.entity.AuditSupervisionTeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditSupervisionTeamMemberRepository extends JpaRepository<AuditSupervisionTeamMember, UUID> {
    List<AuditSupervisionTeamMember> findByTenantIdAndEngagementId(UUID tenantId, UUID engagementId);

    List<AuditSupervisionTeamMember> findByTenantIdAndEngagementIdIn(UUID tenantId, List<UUID> engagementIds);

    List<AuditSupervisionTeamMember> findByTenantIdAndEmployeeId(UUID tenantId, UUID employeeId);

    boolean existsByTenantIdAndEngagementIdAndEmployeeId(UUID tenantId, UUID engagementId, UUID employeeId);

    void deleteByTenantIdAndEngagementId(UUID tenantId, UUID engagementId);
}
