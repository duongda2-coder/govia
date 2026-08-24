package com.govia.audit.riskscoring.masterdata.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Ma tran quy doi diem rui ro: 1 dong ung voi 1 muc tan suat (1-5), 3 cot ung voi 3 muc do
 * nghiem trong (thap/trung binh/cao) - dung de tra ra diem rui ro quy doi (sheet ztc_mtrr_dt).
 */
@Getter
@Setter
@Entity
@Table(name = "risk_score_matrix")
public class RiskMatrix extends BaseEntity {

    @Column(name = "frequency_level", nullable = false)
    private Integer frequencyLevel;

    @Column(name = "frequency_label", nullable = false, length = 255)
    private String frequencyLabel;

    @Column(name = "score_low_severity", precision = 9, scale = 2)
    private BigDecimal scoreLowSeverity;

    @Column(name = "score_medium_severity", precision = 9, scale = 2)
    private BigDecimal scoreMediumSeverity;

    @Column(name = "score_high_severity", precision = 9, scale = 2)
    private BigDecimal scoreHighSeverity;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
