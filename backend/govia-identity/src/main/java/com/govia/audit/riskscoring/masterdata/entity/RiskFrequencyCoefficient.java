package com.govia.audit.riskscoring.masterdata.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** He so tan suat xuat hien sai pham, dung khi cham diem lap lai loi (sheet ZTC_HSSP_DT). */
@Getter
@Setter
@Entity
@Table(name = "risk_score_frequency_coefficient")
public class RiskFrequencyCoefficient extends BaseEntity {

    @Column(name = "code", nullable = false, length = 25)
    private String code;

    @Column(name = "from_year")
    private Integer fromYear;

    @Column(name = "to_year")
    private Integer toYear;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Column(name = "value", precision = 9, scale = 4)
    private BigDecimal value;

    @Column(name = "bonus_point", precision = 9, scale = 2)
    private BigDecimal bonusPoint;

    @Column(name = "repeat_flag", nullable = false)
    private boolean repeat = false;

    /** Luu dang chuoi vi du lieu goc co gia tri dang ">=5" ben canh cac so nguyen 1-4. */
    @Column(name = "repeat_count", length = 10)
    private String repeatCount;

    @Column(name = "repeat_risk_point", precision = 9, scale = 2)
    private BigDecimal repeatRiskPoint;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
