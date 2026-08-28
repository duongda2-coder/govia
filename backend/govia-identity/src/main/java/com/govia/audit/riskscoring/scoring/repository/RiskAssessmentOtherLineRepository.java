package com.govia.audit.riskscoring.scoring.repository;

import com.govia.audit.riskscoring.scoring.entity.RiskAssessmentOtherLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskAssessmentOtherLineRepository extends JpaRepository<RiskAssessmentOtherLine, UUID> {
    List<RiskAssessmentOtherLine> findByHeaderIdOrderByCriteriaOtherIdAsc(UUID headerId);

    Optional<RiskAssessmentOtherLine> findByHeaderIdAndCriteriaOtherId(UUID headerId, UUID criteriaOtherId);
}
