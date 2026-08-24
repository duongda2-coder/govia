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
import java.time.LocalDate;

/** Nhom chi tieu cham diem rui ro cap 1 (sheet ZTC_DGRR_Group1 - bang ZTB_DGRR_GROUP). */
@Getter
@Setter
@Entity
@Table(name = "risk_score_group1")
public class RiskGroup1 extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "object_type", nullable = false, length = 10)
    private ObjectType objectType;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "weight", precision = 9, scale = 4)
    private BigDecimal weight;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
