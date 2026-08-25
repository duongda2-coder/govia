package com.govia.audit.riskscoring.masterdata.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Chi tieu danh gia rui ro dinh tinh (sheet ZTC_CTDGRR_DT). */
@Getter
@Setter
@Entity
@Table(name = "risk_score_criteria_qualitative")
public class RiskCriteriaQualitative extends BaseEntity {

    @Column(name = "audit_object_category_id", nullable = false, columnDefinition = "uuid")
    private UUID auditObjectCategoryId;

    @Column(name = "group1_id", nullable = false, columnDefinition = "uuid")
    private UUID group1Id;

    @Column(name = "group2_id", columnDefinition = "uuid")
    private UUID group2Id;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 1000)
    private String name;

    @Column(name = "weight", precision = 9, scale = 4)
    private BigDecimal weight;

    @Column(name = "impact_level")
    private Integer impactLevel;

    @Column(name = "likelihood_level")
    private Integer likelihoodLevel;

    @Column(name = "include_current_year", nullable = false)
    private boolean includeCurrentYear = true;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
