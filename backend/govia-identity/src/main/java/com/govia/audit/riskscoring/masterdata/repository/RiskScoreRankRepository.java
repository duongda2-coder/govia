package com.govia.audit.riskscoring.masterdata.repository;

import com.govia.audit.riskscoring.masterdata.entity.RiskScoreRank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskScoreRankRepository extends JpaRepository<RiskScoreRank, UUID> {
    List<RiskScoreRank> findByTenantIdOrderByFromYearAscScoreFromAsc(UUID tenantId);

    Optional<RiskScoreRank> findByTenantIdAndRankLabelAndFromYear(UUID tenantId, String rankLabel, Integer fromYear);

    List<RiskScoreRank> findByTenantIdAndRankLabelAndToYear(UUID tenantId, String rankLabel, Integer toYear);
}
