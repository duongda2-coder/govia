package com.govia.audit.riskscoring.masterdata.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Thang diem xep loai rui ro (sheet "QL thang diem", tcode ztc_rank): 1 dong la 1 khoang diem
 * [scoreFrom, scoreTo] ung voi 1 xep loai, hieu luc theo nam. Khi them ky moi (fromYear lon hon)
 * cho cung 1 xep loai, ky cu dang mo (toYear=9999) se tu dong duoc dong lai = fromYear moi - 1,
 * dung theo mo ta trong tai lieu goc.
 */
@Getter
@Setter
@Entity
@Table(name = "risk_score_rank")
public class RiskScoreRank extends BaseEntity {

    @Column(name = "score_from", nullable = false, precision = 10, scale = 2)
    private BigDecimal scoreFrom;

    @Column(name = "score_to", nullable = false, precision = 10, scale = 2)
    private BigDecimal scoreTo;

    @Column(name = "rank_label", nullable = false, length = 50)
    private String rankLabel;

    @Column(name = "from_year", nullable = false)
    private Integer fromYear;

    @Column(name = "to_year", nullable = false)
    private Integer toYear;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
