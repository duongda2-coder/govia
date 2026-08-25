package com.govia.audit.riskscoring.scoring.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Danh muc phan nhom rui ro cua HO theo tuyen bao ve (sheet ZTC_Nhom_DGRR_HO, tcode
 * ztc_nhom_dgrr_ho) - danh muc phang, khong lien ket toi bang nao khac.
 */
@Getter
@Setter
@Entity
@Table(name = "risk_score_group_ho")
public class RiskGroupHO extends BaseEntity {

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
