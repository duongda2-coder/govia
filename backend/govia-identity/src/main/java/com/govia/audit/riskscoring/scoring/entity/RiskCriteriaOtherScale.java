package com.govia.audit.riskscoring.scoring.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Danh muc thang diem cua chi tieu danh gia rui ro HO, CNTT, Du an, Dich vu thue ngoai... (sheet
 * ZTC_CTRR_KHAC_TD, tcode ztc_ctrr_khac_td/ztb_ctrr_khac_td). criteriaOtherId tro toi 1 chi tieu
 * cu the (xem RiskCriteriaOther) - moi chi tieu co the co nhieu dong thang diem (nhieu muc danh
 * gia ung voi cac khoang diem khac nhau).
 */
@Getter
@Setter
@Entity
@Table(name = "risk_score_criteria_other_scale")
public class RiskCriteriaOtherScale extends BaseEntity {

    @Column(name = "audit_object_category_id", nullable = false, columnDefinition = "uuid")
    private UUID auditObjectCategoryId;

    @Column(name = "criteria_other_id", nullable = false, columnDefinition = "uuid")
    private UUID criteriaOtherId;

    @Column(name = "scale_score", nullable = false)
    private Integer scaleScore;

    @Column(name = "rating_level", nullable = false, length = 100)
    private String ratingLevel;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
