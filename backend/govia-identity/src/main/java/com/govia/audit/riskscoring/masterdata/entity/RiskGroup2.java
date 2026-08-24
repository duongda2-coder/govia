package com.govia.audit.riskscoring.masterdata.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Nhom chi tieu cham diem rui ro cap 2, thuoc ve 1 RiskGroup1 (sheet ZTC_DGRR_Group2). */
@Getter
@Setter
@Entity
@Table(name = "risk_score_group2")
public class RiskGroup2 extends BaseEntity {

    @Column(name = "group1_id", nullable = false, columnDefinition = "uuid")
    private UUID group1Id;

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
