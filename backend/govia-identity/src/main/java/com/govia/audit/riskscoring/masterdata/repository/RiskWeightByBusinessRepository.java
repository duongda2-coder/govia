package com.govia.audit.riskscoring.masterdata.repository;

import com.govia.audit.riskscoring.masterdata.entity.RiskWeightByBusiness;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskWeightByBusinessRepository extends JpaRepository<RiskWeightByBusiness, UUID> {
    List<RiskWeightByBusiness> findByTenantIdOrderByBusinessCodeAscFromYearAsc(UUID tenantId);

    Optional<RiskWeightByBusiness> findByTenantIdAndBusinessCodeAndFromYear(UUID tenantId, String businessCode, Integer fromYear);
}
