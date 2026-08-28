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
 * Danh muc chi tieu danh gia rui ro cua HO, CNTT, Du an, Dich vu thue ngoai... (sheet
 * ZTC_CTDGRR_KHAC, tcode ztc_ctdgrr_khac/ztb_ctdgrr_khac). auditObjectCategoryId tro toi danh
 * muc goc "Loai doi tuong kiem toan" (xem AuditObjectCategory); groupHoId/riskTypeHoId tro toi
 * danh muc "Nhom rui ro HO" va "Loai rui ro HO" (xem RiskGroupHO, RiskTypeHO).
 */
@Getter
@Setter
@Entity
@Table(name = "risk_score_criteria_other")
public class RiskCriteriaOther extends BaseEntity {

    @Column(name = "audit_object_category_id", nullable = false, columnDefinition = "uuid")
    private UUID auditObjectCategoryId;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "weight", precision = 9, scale = 4)
    private BigDecimal weight;

    @Column(name = "group_ho_id", nullable = false, columnDefinition = "uuid")
    private UUID groupHoId;

    @Column(name = "risk_type_ho_id", nullable = false, columnDefinition = "uuid")
    private UUID riskTypeHoId;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
