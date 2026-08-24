package com.govia.audit.riskscoring.masterdata.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Ty trong Dinh tinh/Dinh luong theo ma Nghiep vu (sheet ZTC_DLDT_TT). */
@Getter
@Setter
@Entity
@Table(name = "risk_score_weight_by_business")
public class RiskWeightByBusiness extends BaseEntity {

    @Column(name = "business_code", nullable = false, length = 10)
    private String businessCode;

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
