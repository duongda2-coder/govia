package com.govia.audit.riskscoring.scoring.repository;

import com.govia.audit.riskscoring.scoring.entity.RiskBranchScoreExpertRank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskBranchScoreExpertRankRepository extends JpaRepository<RiskBranchScoreExpertRank, UUID> {
    List<RiskBranchScoreExpertRank> findByTenantIdAndYearOrderByTotalScoreDesc(UUID tenantId, Integer year);

    Optional<RiskBranchScoreExpertRank> findByTenantIdAndBranchCodeAndYear(UUID tenantId, String branchCode, Integer year);
}
