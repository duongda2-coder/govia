package com.govia.audit.riskscoring.masterdata.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Chi tieu danh gia rui ro dinh luong, co 5 moc diem 20/40/60/80/100 (sheet ZTC_CTDGRR_DL). */
@Getter
@Setter
@Entity
@Table(name = "risk_score_criteria_quantitative")
public class RiskCriteriaQuantitative extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "object_type", nullable = false, length = 10)
    private ObjectType objectType;

    @Column(name = "group1_id", nullable = false, columnDefinition = "uuid")
    private UUID group1Id;

    @Column(name = "group2_id", columnDefinition = "uuid")
    private UUID group2Id;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 1000)
    private String name;

    @Column(name = "criteria_type")
    private Integer criteriaType;

    @Column(name = "business_threshold", precision = 12, scale = 4)
    private BigDecimal businessThreshold;

    @Column(name = "view_threshold", precision = 12, scale = 4)
    private BigDecimal viewThreshold;

    @Column(name = "score_20", precision = 12, scale = 4)
    private BigDecimal score20;

    @Column(name = "score_40", precision = 12, scale = 4)
    private BigDecimal score40;

    @Column(name = "score_60", precision = 12, scale = 4)
    private BigDecimal score60;

    @Column(name = "score_80", precision = 12, scale = 4)
    private BigDecimal score80;

    @Column(name = "score_100", precision = 12, scale = 4)
    private BigDecimal score100;

    @Column(name = "scoring_guide", length = 2000)
    private String scoringGuide;

    @Column(name = "include_current_year", nullable = false)
    private boolean includeCurrentYear = true;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
