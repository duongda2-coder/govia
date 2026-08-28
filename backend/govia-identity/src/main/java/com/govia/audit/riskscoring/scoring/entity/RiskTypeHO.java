package com.govia.audit.riskscoring.scoring.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Danh muc loai rui ro cua HO (sheet ZTC_RR_HO, tcode ztc_rr_ho/ztb_rr_ho) - groupHoId tro toi
 * danh muc cha "Nhom rui ro HO theo tuyen bao ve" (xem RiskGroupHO).
 */
@Getter
@Setter
@Entity
@Table(name = "risk_score_type_ho")
public class RiskTypeHO extends BaseEntity {

    @Column(name = "group_ho_id", nullable = false, columnDefinition = "uuid")
    private UUID groupHoId;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "weight", precision = 9, scale = 4)
    private BigDecimal weight;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
