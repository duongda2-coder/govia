package com.govia.audit.riskscoring.masterdata.repository;

import com.govia.audit.riskscoring.masterdata.entity.RiskWeightByBusinessSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskWeightByBusinessSegmentRepository extends JpaRepository<RiskWeightByBusinessSegment, UUID> {
    List<RiskWeightByBusinessSegment> findByTenantIdOrderBySegmentCodeAscFromYearAsc(UUID tenantId);

    Optional<RiskWeightByBusinessSegment> findByTenantIdAndSegmentCodeAndFromYear(UUID tenantId, String segmentCode, Integer fromYear);
}
