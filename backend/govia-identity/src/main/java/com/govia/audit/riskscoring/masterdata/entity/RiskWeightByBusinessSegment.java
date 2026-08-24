package com.govia.audit.riskscoring.masterdata.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Ty trong Dinh tinh/Dinh luong theo Ma mang nghiep vu (sheet ZTC_DTDL_TT). */
@Getter
@Setter
@Entity
@Table(name = "risk_score_weight_by_business_segment")
public class RiskWeightByBusinessSegment extends BaseEntity {

    @Column(name = "segment_code", nullable = false, length = 20)
    private String segmentCode;

    @Column(name = "qualitative_weight", precision = 9, scale = 4)
    private BigDecimal qualitativeWeight;

    @Column(name = "quantitative_weight", precision = 9, scale = 4)
    private BigDecimal quantitativeWeight;

    @Column(name = "from_year")
    private Integer fromYear;

    @Column(name = "to_year")
    private Integer toYear;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
